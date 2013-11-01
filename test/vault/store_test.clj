(ns vault.store-test
  (:require [clojure.test :refer :all]
            [vault.blob :as blob]
            [vault.store :refer :all]))


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
