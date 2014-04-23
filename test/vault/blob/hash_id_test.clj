(ns vault.blob.hash-id-test
  (:require
    [clojure.test :refer :all]
    [vault.blob.core :as blob]))


(def blob-content
  (.getBytes "foobarbaz"))

(def blob-id
  (blob/hash-id :sha256 "97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"))

(def blob-id-str
  "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d")


(deftest blob-digest-algorithm
  (is (not (nil? blob/*hash-algorithm*)))
  (blob/with-algorithm :sha1
    (is (= :sha1 blob/*hash-algorithm*))))


(deftest hash-id-comparison
  (let [b1 (blob/hash-id :sha1 "33655e63cafac69a5287e96f71457bbfa6d7deec")
        b2 (blob/hash-id :sha256 "14ea6507645c2ba7e973ea87444bf0470fc2e1f4b64f4f692f736acf9a4dec8a")
        b3 (blob/hash-id :sha256 "fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9")]
    (testing "comparison"
      (is (= 0 (compare b1 b1)) "is reflexive")
      (is (= 0 (compare b2 (blob/hash-id (:algorithm b2) (:digest b2))))
          "gives zero for equal hash ids")
      (testing "between algorithms"
        (is (> 0 (compare b1 b2)))
        (is (< 0 (compare b2 b1))))
      (testing "between digests"
        (is (> 0 (compare b2 b3)))
        (is (< 0 (compare b3 b2)))))))


(deftest hash-id-strings
  (testing "string representation"
    (is (= blob-id-str (str blob-id)))
    (is (= "sha256-97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"
           (blob/path-str blob-id)))))


(deftest identifier-parsing
  (testing "parse-identifier"
    (are [addr] (= (blob/hash-id addr) blob-id)
         (str "urn:hash:" blob-id-str)
         (str "urn:" blob-id-str)
         blob-id-str)))


(deftest hash-id-coercion
  (testing "blob/hash-id"
    (is (identical? (blob/hash-id blob-id) blob-id))
    (is (= (blob/hash-id blob-id-str) blob-id))
    (is (= (blob/hash-id :sha256 (:digest blob-id)) blob-id))))


(deftest content-hashing
  (testing "blob/hash"
    (is (= blob-id (blob/hash :sha256 blob-content)))
    (is (thrown? AssertionError (blob/hash :foo blob-content)))))


(deftest hash-id-selection
  (let [a (blob/hash-id :md5 "37b51d194a7513e45b56f6524f2d51f2")
        b (blob/hash-id :md5 "73fcffa4b7f6bb68e44cf984c85f6e88")
        c (blob/hash-id :md5 "73fe285cedef654fccc4a4d818db4cc2")
        d (blob/hash-id :md5 "acbd18db4cc2f85cedef654fccc4a4d8")
        e (blob/hash-id :md5 "c3c23db5285662ef7172373df0003206")
        hash-ids [a b c d e]]
    (are [brs opts] (= brs (blob/select-ids opts hash-ids))
         hash-ids {}
         [c d e]  {:after "md5:73fd2"}
         [b c]    {:prefix "md5:73"}
         [a b]    {:limit 2})))
