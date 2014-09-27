(ns vault.blob.content-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.test :refer :all]
    [vault.blob.content :as content])
  (:import
    java.io.ByteArrayOutputStream))


(def blob-content
  (.getBytes "foobarbaz"))

(def blob-id
  (content/hash-id :sha256 "97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"))

(def blob-id-str
  "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d")


(deftest hash-id-comparison
  (let [b1 (content/hash-id :sha1 "33655e63cafac69a5287e96f71457bbfa6d7deec")
        b2 (content/hash-id :sha256 "14ea6507645c2ba7e973ea87444bf0470fc2e1f4b64f4f692f736acf9a4dec8a")
        b3 (content/hash-id :sha256 "fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9")]
    (testing "comparison"
      (is (= 0 (compare b1 b1)) "is reflexive")
      (is (= 0 (compare b2 (content/hash-id (:algorithm b2) (:digest b2))))
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
           (content/path-str blob-id)))))


(deftest identifier-parsing
  (testing "parse-identifier"
    (are [addr] (= (content/hash-id addr) blob-id)
         (str "urn:hash:" blob-id-str)
         (str "urn:" blob-id-str)
         blob-id-str)))


(deftest hash-id-coercion
  (testing "content/hash-id"
    (is (identical? (content/hash-id blob-id) blob-id))
    (is (= (content/hash-id blob-id-str) blob-id))
    (is (= (content/hash-id :sha256 (:digest blob-id)) blob-id))))


(deftest zero-padding
  (let [hex (content/hex-str (byte-array 4))]
    (is (= "00000000" hex))))


(deftest content-hashing
  (testing "content/hash"
    (is (= blob-id (content/hash :sha256 blob-content)))
    (content/with-digest :md5
      (is (= :md5 (:algorithm (content/hash blob-content)))))
    (is (thrown? AssertionError (content/hash :foo blob-content)))))


(deftest blob-io
  (is (nil? (content/read (byte-array 0))))
  (is (nil? (content/read "")))
  (let [content (.getBytes "foo")
        blob (content/read content)]
    (is (not (nil? blob)))
    (is (not (nil? (:id blob))))
    (is (not (empty? (:content blob))))
    (let [buffer (ByteArrayOutputStream.)]
      (content/write blob buffer)
      (is (bytes= content (.toByteArray buffer))))))
