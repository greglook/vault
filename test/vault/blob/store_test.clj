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
       (map (juxt (partial blob/put! store) identity))
       (into (sorted-map))))


(defn- test-blob-content
  "Determines whether the store contains the content for the given identifier."
  [store id content]
  (let [status (blob/stat store id)
        stored-content (with-open [stream (blob/open store id)]
                         (slurp stream))]
    (is (and status stored-content) "returns info and content")
    (is (= (:size status) (count (.getBytes stored-content))) "stats contain size info")
    (is (= content stored-content) "stored content matches input")))


(defn- test-restore-blob
  "Tests re-storing an existing blob."
  [store id content]
  (is (blob/contains? store id))
  (let [status     (blob/stat store id)
        new-id     (blob/put! store content)
        new-status (blob/stat store id)]
    (is (= id new-id))
    (is (= (:stored-at status)
           (:stored-at new-status)))))


(defn test-blob-store
  "Tests a blob store implementation."
  [store]
  (is (empty? (blob/list store)) "starts empty")
  (testing (str (-> store class .getSimpleName))
    (let [blobs (store-test-blobs! store)]
      (is (= (keys blobs) (blob/list store {}))
          "enumerates all ids in sorted order")
      (doseq [[id content] blobs]
        (test-blob-content store id content))
      (let [[id content] (first (seq blobs))]
        (test-restore-blob store id content))
      (doseq [id (keys blobs)]
        (is (blob/delete! store id) "delete returns true"))
      (is (empty? (blob/list store)) "ends empty")
      (is (not (blob/delete! store (first (keys blobs))))
          "gives false when removing a nonexistent blob"))))


(deftest memory-blob-store
  (let [store (memory-store)]
    (test-blob-store (memory-store))
    (blob/destroy!! store)))


(deftest file-blob-store
  (let [tmpdir (io/file "target" "test" "tmp"
                        (str "file-blob-store."
                             (System/currentTimeMillis)))
        store (file-store tmpdir)]
    (test-blob-store (file-store tmpdir))
    (blob/destroy!! store)))
