(ns vault.blob.store-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    (vault.blob
      [core :as blob]
      [store :as store])
    (vault.blob.store
      [memory :refer [memory-store]]
      [file :refer [file-store]])))


;; STORAGE FUNCTION TESTS

(deftest blob-loading
  (is (nil? (blob/load (byte-array 0))))
  (is (nil? (blob/load "")))
  (let [blob (blob/load "foo")]
    (is (not (nil? blob)))
    (is (not (nil? (:id blob))))
    (is (not (empty? (:content blob))))))


(deftest list-wrapper
  (let [store (reify store/BlobStore (enumerate [this opts] (vector :list opts)))]
    (is (= [:list nil] (blob/list store)))
    (is (= [:list {:foo "bar" :baz 3}] (blob/list store :foo "bar" :baz 3)))))


(deftest get-wrapper
  (let [content (.getBytes "foobarbaz")
        id (blob/hash :sha256 content)
        store (reify store/BlobStore (get* [this id] (blob/load content)))
        blob (blob/get store id)]
    (is (= id (:id blob)))
    (is (bytes= content (:content blob)))
    (is (thrown? RuntimeException
                 (blob/get store (:id (blob/load "bazbarfoo")))))))


(deftest hash-id-selection
  (let [a (blob/hash-id :md5 "37b51d194a7513e45b56f6524f2d51f2")
        b (blob/hash-id :md5 "73fcffa4b7f6bb68e44cf984c85f6e88")
        c (blob/hash-id :md5 "73fe285cedef654fccc4a4d818db4cc2")
        d (blob/hash-id :md5 "acbd18db4cc2f85cedef654fccc4a4d8")
        e (blob/hash-id :md5 "c3c23db5285662ef7172373df0003206")
        hash-ids [a b c d e]]
    (are [brs opts] (= brs (store/select-ids opts hash-ids))
         hash-ids {}
         [c d e]  {:after "md5:73fd2"}
         [b c]    {:prefix "md5:73"}
         [a b]    {:limit 2})))



;; STORAGE IMPLEMENTATION TESTS

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
    (is (= (:stat/size status) (count stored-content)) "stats contain size info")
    (is (= content (slurp stored-content)) "stored content matches input")))


(defn- test-restore-blob
  "Tests re-storing an existing blob."
  [store id content]
  (let [status     (blob/stat store id)
        new-blob   (blob/store! store content)
        new-status (blob/stat store id)]
    (is (= id (:id new-blob)))
    (is (= (:stat/stored-at status)
           (:stat/stored-at new-status)))))


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


(deftest test-memory-blob-store
  (let [store (memory-store)]
    (test-blob-store (memory-store))
    (store/destroy!! store)))


(defn test-file-blob-store
  []
  (let [tmpdir (io/file "target" "test" "tmp"
                        (str "file-blob-store."
                             (System/currentTimeMillis)))
        store (file-store tmpdir)]
    (test-blob-store (file-store tmpdir))
    (store/destroy!! store)))
