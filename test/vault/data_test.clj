(ns vault.data-test
  (:require
    [clojure.test :refer :all]
    [vault.data :refer :all])
  (:import
    java.io.ByteArrayInputStream))


(defn read-data-string
  "Reads data from bytes from a string."
  [string]
  (-> string
      (.getBytes blob-charset)
      ByteArrayInputStream.
      read-data))


(deftest read-non-data-blob
  (let [content "foobarbaz not a data blob"
        result (read-data-string content)]
    (is (instance? java.io.InputStream result))
    (is (= content (slurp result)))))


(deftest read-data-blob
  (let [content "#vault/data\n[:foo]"
        result (read-data-string content)]
    (is (= [:foo] result))))


(deftest read-primary-bytes
  (binding [*primary-bytes* nil]
    (let [primary-content "[\\x \\y \\z]"
          content (str "#vault/data\n" primary-content)
          result (read-data-string content)]
      (is (= [\x \y \z] result))
      (is (not (nil? *primary-bytes*)))
      (is (bytes= (.getBytes primary-content blob-charset) *primary-bytes*)))))


(deftest read-primary-bytes-with-extra-values
  (binding [*primary-bytes* nil]
    (let [primary-content "#{:bar \"baz\"}"
          content (str "#vault/data\n" primary-content "\n\n:frobble")
          result (read-data-string content)]
      (is (= #{:bar "baz"} result))
      (is (not (nil? *primary-bytes*)))
      (is (bytes= (.getBytes primary-content blob-charset) *primary-bytes*)))))
