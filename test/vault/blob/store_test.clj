(ns vault.blob.store-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [vault.blob.content :as content]
    [vault.blob.store :as store]
    [vault.blob.store.file :refer [file-store]]
    [vault.blob.store.memory :refer [memory-store]]))


;; ## Storage Function Tests

(deftest list-wrapper
  (let [store (reify store/BlobStore (enumerate [this opts] (vector :list opts)))]
    (is (= [:list nil] (store/list store)))
    (is (= [:list {:foo "bar" :baz 3}] (store/list store :foo "bar" :baz 3)))))


(deftest checked-get
  (let [content (.getBytes "foobarbaz")
        id (content/hash :sha256 content)
        store (reify store/BlobStore (get* [this id] (content/read content)))
        blob (store/get* store id)]
    (is (= id (:id blob)))
    (is (bytes= content (:content blob)))
    (is (thrown? RuntimeException
                 (store/get store (:id (content/read "bazbarfoo")))))))


(deftest hash-id-selection
  (let [a (content/hash-id :md5 "37b51d194a7513e45b56f6524f2d51f2")
        b (content/hash-id :md5 "73fcffa4b7f6bb68e44cf984c85f6e88")
        c (content/hash-id :md5 "73fe285cedef654fccc4a4d818db4cc2")
        d (content/hash-id :md5 "acbd18db4cc2f85cedef654fccc4a4d8")
        e (content/hash-id :md5 "c3c23db5285662ef7172373df0003206")
        hash-ids [a b c d e]]
    (are [brs opts] (= brs (store/select-ids opts hash-ids))
         hash-ids {}
         [c d e]  {:after "md5:73fd2"}
         [b c]    {:prefix "md5:73"}
         [a b]    {:limit 2})))



;; ## Blob Store Tests

(defn- store-test-blobs!
  "Stores some test blobs in the given blob store and returns a map of the
  ids to the original string values."
  [store]
  (->> ["foo" "bar" "baz" "foobar" "barbaz"]
       (map (juxt (comp :id (partial store/store! store)) identity))
       (into (sorted-map))))


(defn- test-blob-content
  "Determines whether the store contains the content for the given identifier."
  [store id content]
  (let [status (store/stat store id)
        stored-content (:content (store/get store id))]
    (is (and status stored-content) "returns info and content")
    (is (= (:stat/size status) (count stored-content)) "stats contain size info")
    (is (= content (slurp stored-content)) "stored content matches input")))


(defn- test-restore-blob
  "Tests re-storing an existing blob."
  [store id content]
  (let [status     (store/stat store id)
        new-blob   (store/store! store content)
        new-status (store/stat store id)]
    (is (= id (:id new-blob)))
    (is (= (:stat/stored-at status)
           (:stat/stored-at new-status)))))


(defn test-blob-store
  "Tests a blob store implementation."
  [store label]
  (println "  *" label)
  (is (empty? (store/list store)) "starts empty")
  (testing (str (-> store class .getSimpleName))
    (let [stored-content (store-test-blobs! store)]
      (is (= (keys stored-content) (store/list store {}))
          "enumerates all ids in sorted order")
      (doseq [[id content] stored-content]
        (test-blob-content store id content))
      (let [[id content] (first (seq stored-content))]
        (test-restore-blob store id content))
      (let [expected-size (reduce + 0 (map (comp count #(.getBytes %))
                                           (vals stored-content)))]
        (is (= expected-size (store/scan-size store))))
      (doseq [id (keys stored-content)]
        (store/delete! store id))
      (is (empty? (store/list store)) "ends empty"))))



;; ## Storage Implementations

(deftest test-memory-store
  (let [store (memory-store)]
    (test-blob-store store "memory-store")))


(deftest test-file-store
  (let [tmpdir (io/file "target" "test" "tmp"
                        (str "file-blob-store."
                          (System/currentTimeMillis)))
        store (file-store tmpdir)]
    (test-blob-store store "file-store")
    (store/erase!! store)))
