(ns vault.blob-test
  (:require [clojure.test :refer :all]
            [vault.blob :refer :all]
            [vault.data :as data]))


(def blob-content
  "foobarbaz")

(def blob-ref
  (->BlobRef :sha256 "97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"))

(def blob-address
  "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d")


(deftest blobref-comparison
  (let [br1 (make-blobref :sha1 "33655e63cafac69a5287e96f71457bbfa6d7deec")
        br2 (make-blobref :sha256 "14ea6507645c2ba7e973ea87444bf0470fc2e1f4b64f4f692f736acf9a4dec8a")
        br3 (make-blobref :sha256 "fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9")]
    (testing "comparison"
      (is (= 0 (compare br1 br1)) "is reflexive")
      (is (= 0 (compare br2 (make-blobref (:algorithm br2) (:digest br2))))
          "gives zero for equal blobrefs")
      (testing "between algorithms"
        (is (> 0 (compare br1 br2)))
        (is (< 0 (compare br2 br1))))
      (testing "between digests"
        (is (> 0 (compare br2 br3)))
        (is (< 0 (compare br3 br2)))))))


(deftest blobref-strings
  (testing "string representation"
    (is (= (str blob-ref) blob-address)))
  (testing "print representation"
    (is (= (with-out-str (pr blob-ref)) (str "#vault/ref " \" blob-address \")))))


(deftest edn-representation
  (is (= 'vault/ref (data/tag blob-ref)))
  (is (= blob-address (data/value blob-ref))))


(deftest address-parsing
  (testing "parse-address"
    (are [addr] (= (parse-address addr) blob-ref)
         (str "urn:hash:" blob-address)
         (str "urn:" blob-address)
         blob-address)
    (is (thrown? IllegalArgumentException (parse-address "abc1:a19d14f8e")))))


(deftest content-hashing-test
  (testing "hash-content"
    (is (= (hash-content :sha256 blob-content) blob-ref))
    (is (thrown? IllegalArgumentException (hash-content :sha4 "xyz")))))


(deftest make-blobref-test
  (testing "make-blobref"
    (is (identical? (make-blobref blob-ref) blob-ref))
    (is (= (make-blobref blob-address) blob-ref))
    (is (= (make-blobref :sha256 (:digest blob-ref)) blob-ref))))
