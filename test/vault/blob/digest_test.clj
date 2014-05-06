(ns vault.blob.digest-test
  (:require
    [clojure.test :refer :all]
    (vault.blob
      [digest :as digest])))


(def blob-content
  (.getBytes "foobarbaz"))

(def blob-id
  (digest/hash-id :sha256 "97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"))

(def blob-id-str
  "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d")


(deftest hash-id-comparison
  (let [b1 (digest/hash-id :sha1 "33655e63cafac69a5287e96f71457bbfa6d7deec")
        b2 (digest/hash-id :sha256 "14ea6507645c2ba7e973ea87444bf0470fc2e1f4b64f4f692f736acf9a4dec8a")
        b3 (digest/hash-id :sha256 "fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9")]
    (testing "comparison"
      (is (= 0 (compare b1 b1)) "is reflexive")
      (is (= 0 (compare b2 (digest/hash-id (:algorithm b2) (:digest b2))))
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
           (digest/path-str blob-id)))))


(deftest identifier-parsing
  (testing "parse-identifier"
    (are [addr] (= (digest/hash-id addr) blob-id)
         (str "urn:hash:" blob-id-str)
         (str "urn:" blob-id-str)
         blob-id-str)))


(deftest hash-id-coercion
  (testing "digest/hash-id"
    (is (identical? (digest/hash-id blob-id) blob-id))
    (is (= (digest/hash-id blob-id-str) blob-id))
    (is (= (digest/hash-id :sha256 (:digest blob-id)) blob-id))))


(deftest content-hashing
  (testing "digest/hash"
    (is (= blob-id (digest/hash :sha256 blob-content)))
    (is (thrown? AssertionError (digest/hash :foo blob-content)))))
