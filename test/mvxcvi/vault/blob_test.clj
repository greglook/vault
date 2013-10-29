(ns mvxcvi.vault.blob-test
  (:require [clojure.test :refer :all]
            [mvxcvi.vault.blob :refer :all]))


(def blob-content
  "foobarbaz")

(def blob-ref
  (->BlobRef :sha256 "97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"))

(def blob-address
  "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d")


(deftest blobref-strings
  (testing "BlobRef"
    (testing "string representation"
      (is (= (str blob-ref) blob-address)))
    (testing "print representation"
      (is (= (with-out-str (pr blob-ref)) (str "#vault/ref " \" blob-address \"))))))


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
