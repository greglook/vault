(ns vault.store-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [vault.blob :as blob]
            [vault.store :refer :all]
            (vault.store
              [memory :refer [memory-store]]
              [file :refer [file-store]])))


(deftest blob-containment
  (let [blobref (blob/hash-content :sha1 "foo bar")]
    (is (false? (contains-blob? (reify BlobStore (stat [this blobref] nil)) blobref)))
    (is (true? (contains-blob? (reify BlobStore (stat [this blobref] {:size 1})) blobref)))))


(deftest blobref-selection
  (let [a (blob/make-blobref :md5 "37b51d194a7513e45b56f6524f2d51f2")
        b (blob/make-blobref :md5 "73fcffa4b7f6bb68e44cf984c85f6e88")
        c (blob/make-blobref :md5 "73fe285cedef654fccc4a4d818db4cc2")
        d (blob/make-blobref :md5 "acbd18db4cc2f85cedef654fccc4a4d8")
        e (blob/make-blobref :md5 "c3c23db5285662ef7172373df0003206")
        blobrefs [a b c d e]]
    (are [brs opts] (= brs (select-blobrefs opts blobrefs))
         blobrefs {}
         [c d e]  {:start "md5:73fd2"}
         [b c]    {:prefix "md5:73"}
         [a b]    {:count 2})))


(defn- store-test-blobs!
  "Stores some test blobs in the given blob store and returns a map of the
  blobrefs to the original string values."
  [store]
  (->> ["foo" "bar" "baz" "foobar" "barbaz"]
       (map (juxt (partial store! store) identity))
       (into (sorted-map))))


(defn- test-stored-blob
  "Determines whether the store contains the data at the given address."
  [store blobref data]
  (let [blob-info (stat store blobref)
        stored-content (with-open [stream (open store blobref)]
                         (slurp stream))]
    (is (and blob-info stored-content) "returns info and content")
    (is (= (:size blob-info) (count (.getBytes stored-content))) "stats contain size info")
    (is (= data stored-content) "content matches data")))


(defn test-blob-store
  "Tests a blob store implementation."
  [store-factory]
  (doseq [algo blob/digest-algorithms]
    (let [store (store-factory algo)]
      (testing (str (-> store class .getSimpleName) " with " (name algo))
        (is (= algo (algorithm store)) "reports algorithm correctly")
        (is (empty? (enumerate store)) "starts empty")
        (let [blobs (store-test-blobs! store)]
          (is (= (keys blobs) (enumerate store))
              "enumerates all blobrefs in sorted order")
          ; TODO: test expected subsequences? e.g. options to `enumerate`
          (doseq [[blobref data] blobs]
            (testing (str "for blob " blobref)
              (test-stored-blob store blobref data)
              (is (remove! store blobref) "remove returns true")))
          (is (empty? (enumerate store)) "ends empty")
          (is (not (remove! store (first (keys blobs))))
              "gives false when removing a nonexistent blob"))))))


(deftest memory-blob-store
  (test-blob-store memory-store))


(deftest file-blob-store
  (let [tmpdir (io/file "target" "test" "tmp"
                        (str "file-blob-store."
                             (System/currentTimeMillis)))]
    (.mkdirs tmpdir)
    (test-blob-store #(file-store % tmpdir))))
