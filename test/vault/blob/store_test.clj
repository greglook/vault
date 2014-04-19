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
       (map (juxt (comp :id (partial blob/store! store)) identity))
       (into (sorted-map))))


(defn- test-blob-content
  "Determines whether the store contains the content for the given identifier."
  [store id content]
  (let [status (blob/stat store id)
        stored-content (:content (blob/get store id))]
    (is (and status stored-content) "returns info and content")
    (is (= (:meta/size status) (count stored-content)) "stats contain size info")
    (is (= content (slurp stored-content)) "stored content matches input")))


(defn- test-restore-blob
  "Tests re-storing an existing blob."
  [store id content]
  (let [status     (blob/stat store id)
        new-blob   (blob/store! store content)
        new-status (blob/stat store id)]
    (is (= id (:id new-blob)))
    (is (= (:meta/stored-at status)
           (:meta/stored-at new-status)))))


(defn test-blob-store
  "Tests a blob store implementation."
  [store]
  (is (empty? (blob/list store)) "starts empty")
  (testing (str (-> store class .getSimpleName))
    (let [stored-content (store-test-blobs! store)]
      (is (= (keys stored-content) (blob/list store {}))
          "enumerates all ids in sorted order")
      (doseq [[id content] stored-content]
        (test-blob-content store id content))
      (let [[id content] (first (seq stored-content))]
        (test-restore-blob store id content))
      #_
      (doseq [id (keys blobs)]
        (is (blob/delete! store id) "delete returns true"))
      #_
      (is (empty? (blob/list store)) "ends empty")
      #_
      (is (not (blob/delete! store (first (keys blobs))))
          "gives false when removing a nonexistent blob"))))


(deftest memory-blob-store
  (let [store (memory-store)]
    (test-blob-store (memory-store))
    (vault.blob.store.memory/destroy!! store)))


#_
(deftest file-blob-store
  (let [tmpdir (io/file "target" "test" "tmp"
                        (str "file-blob-store."
                             (System/currentTimeMillis)))
        store (file-store tmpdir)]
    (test-blob-store (file-store tmpdir))
    (vault.blob.store.file/destroy!! store)))
