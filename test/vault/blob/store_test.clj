(ns vault.blob.store-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    (vault.blob
      [core :as blob])
    (vault.blob.store
      [memory :refer [memory-store]]
      [file :refer [file-store]])))


(defn- store-test-blobs!
  "Stores some test blobs in the given blob store and returns a map of the
  ids to the original string values."
  [store]
  (->> ["foo" "bar" "baz" "foobar" "barbaz"]
       (map (juxt (partial blob/store! store) identity))
       (into (sorted-map))))


(defn- test-stored-blob
  "Determines whether the store contains the data for the given identifier"
  [store id data]
  (let [status (blob/stat store id)
        stored-content (with-open [stream (blob/open store id)]
                         (slurp stream))]
    (is (and status stored-content) "returns info and content")
    (is (= (:size status) (count (.getBytes stored-content))) "stats contain size info")
    (is (= data stored-content) "content matches data")))


(defn test-blob-store
  "Tests a blob store implementation."
  [store]
  (is (empty? (blob/list store)) "starts empty")
  (testing (str (-> store class .getSimpleName))
    (let [blobs (store-test-blobs! store)]
      (is (= (keys blobs) (blob/list store {}))
          "enumerates all ids in sorted order")
      (doseq [[id data] blobs]
        (testing (str "for blob " id)
          (test-stored-blob store id data)
          (is (blob/remove! store id) "remove returns true")))
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
