(ns vault.blob.store-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [vault.blob.core :as blob :refer [BlobStore]]
    (vault.blob.store
      [memory :refer [memory-store]]
      [file :refer [file-store]])))


;; STORAGE INTERFACE TESTS

(deftest list-wrapper
  (let [store (reify BlobStore (-list [this opts] (vector :list opts)))]
    (is (= [:list nil] (blob/list store)))
    (is (= [:list {:foo "bar" :baz 3}] (blob/list store :foo "bar" :baz 3)))))


(deftest stat-wrapper
  (let [store (reify BlobStore (-stat [this blobref] (vector :stat blobref)))]
    (is (= [:stat :blobref] (blob/stat store :blobref)))))


(deftest contains?-wrapper
  (let [store (reify BlobStore (-stat [this blobref] nil))]
    (is (false? (blob/contains? store :blobref))))
  (let [store (reify BlobStore (-stat [this blobref] {:size 1}))]
    (is (true? (blob/contains? store :blobref)))))


(deftest open-wrapper
  (let [store (reify BlobStore (-open [this blobref] (vector :open blobref)))]
    (is (= [:open :blobref] (blob/open store :blobref)))))


(deftest store!-wrapper
  (let [store (reify BlobStore (-store! [this blobref status stream] (vector :store stream)))]
    (is (= [:store :content] (blob/store! store :content)))))


(deftest remove!-wrapper
  (let [store (reify BlobStore (-remove! [this blobref] (vector :remove blobref)))]
    (is (= [:remove :blobref] (blob/remove! store :blobref)))))



;; INTEGRATION TESTS

(defn- store-test-blobs!
  "Stores some test blobs in the given blob store and returns a map of the
  blobrefs to the original string values."
  [store]
  (->> ["foo" "bar" "baz" "foobar" "barbaz"]
       (map (juxt (partial blob/store! store) identity))
       (into (sorted-map))))


(defn- test-stored-blob
  "Determines whether the store contains the data for the given identifier"
  [store blobref data]
  (let [blob-info (blob/stat store blobref)
        stored-content (with-open [stream (blob/open store blobref)]
                         (slurp stream))]
    (is (and blob-info stored-content) "returns info and content")
    (is (= (:size blob-info) (count (.getBytes stored-content))) "stats contain size info")
    (is (= data stored-content) "content matches data")))


(defn test-blob-store
  "Tests a blob store implementation."
  [store]
  (is (empty? (blob/list store)) "starts empty")
  (testing (str (-> store class .getSimpleName))
    (let [blobs (store-test-blobs! store)]
      (is (= (keys blobs) (blob/list store {}))
          "enumerates all blobrefs in sorted order")
      (doseq [[blobref data] blobs]
        (testing (str "for blob " blobref)
          (test-stored-blob store blobref data)
          (is (blob/remove! store blobref) "remove returns true")))
      (is (empty? (blob/list store)) "ends empty")
      (is (not (blob/remove! store (first (keys blobs))))
          "gives false when removing a nonexistent blob"))))


(deftest memory-blob-store
  (test-blob-store (memory-store)))


(deftest file-blob-store
  (let [tmpdir (io/file "target" "test" "tmp"
                        (str "file-blob-store."
                             (System/currentTimeMillis)))]
    (.mkdirs tmpdir)
    (test-blob-store (file-store tmpdir))))
