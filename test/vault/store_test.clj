(ns vault.store-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [vault.blob :as blob :refer [BlobStore]]
            (vault.store
              [memory :refer [memory-store]]
              [file :refer [file-store]])))


;; STORAGE INTERFACE TESTS

(deftest list-wrapper
  (let [store (reify BlobStore (-list [this opts] (vector :list opts)))]
    (blob/with-blob-store store
      (is (= [:list nil]
             (blob/list)
             (blob/list store)
             (blob/list nil)))
      (is (= [:list {:foo "bar" :baz 3}]
             (blob/list {:foo "bar" :baz 3})
             (blob/list store :foo "bar" :baz 3))))))


(deftest stat-wrapper
  (let [store (reify BlobStore (-stat [this blobref] (vector :stat blobref)))]
    (blob/with-blob-store store
      (is (= [:stat :blobref]
             (blob/stat store :blobref)
             (blob/stat :blobref))))))


(deftest contains?-wrapper
  (let [store (reify BlobStore (-stat [this blobref] nil))]
    (blob/with-blob-store store
      (is (false? (blob/contains? store :blobref)))
      (is (false? (blob/contains? :blobref)))))
  (let [store (reify BlobStore (-stat [this blobref] {:size 1}))]
    (is (true? (blob/contains? store :blobref)))))


(deftest open-wrapper
  (let [store (reify BlobStore (-open [this blobref] (vector :open blobref)))]
    (blob/with-blob-store store
      (is (= [:open :blobref]
             (blob/open store :blobref)
             (blob/open :blobref))))))


(deftest store!-wrapper
  (let [store (reify BlobStore (-store! [this content] (vector :store content)))]
    (blob/with-blob-store store
      (is (= [:store :content]
             (blob/store! store :content)
             (blob/store! :content))))))


(deftest remove!-wrapper
  (let [store (reify BlobStore (-remove! [this blobref] (vector :remove blobref)))]
    (blob/with-blob-store store
      (is (= [:remove :blobref]
             (blob/remove! store :blobref)
             (blob/remove! :blobref))))))



;; INTEGRATION TESTS

(defn- store-test-blobs!
  "Stores some test blobs in the given blob store and returns a map of the
  blobrefs to the original string values."
  [store algorithm]
  (blob/with-digest-algorithm algorithm
    (->> ["foo" "bar" "baz" "foobar" "barbaz"]
         (map (juxt (partial blob/store! store) identity))
         (into (sorted-map)))))


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
  (blob/with-blob-store store
    (is (empty? (blob/list store)) "starts empty")
    (doseq [algo blob/digest-algorithms]
      (testing (str (-> store class .getSimpleName) " with " (name algo))
        (let [blobs (store-test-blobs! store algo)]
          (is (= (keys blobs) (blob/list store {}))
              "enumerates all blobrefs in sorted order")
          ; TODO: test expected subsequences? e.g. options to `enumerate`
          (doseq [[blobref data] blobs]
            (testing (str "for blob " blobref)
              (test-stored-blob store blobref data)
              (is (blob/remove! store blobref) "remove returns true")))
          (is (empty? (blob/list store)) "ends empty")
          (is (not (blob/remove! store (first (keys blobs))))
              "gives false when removing a nonexistent blob"))))))


(deftest memory-blob-store
  (test-blob-store (memory-store)))


(deftest file-blob-store
  (let [tmpdir (io/file "target" "test" "tmp"
                        (str "file-blob-store."
                             (System/currentTimeMillis)))]
    (.mkdirs tmpdir)
    (test-blob-store (file-store tmpdir))))
