(ns vault.data-test
  (:require
    [clojure.test :refer :all]
    [vault.data :refer :all])
  (:import
    java.io.ByteArrayInputStream))


(defn string-input
  "Creates an InputStream which reads bytes from a string."
  [string]
  (ByteArrayInputStream. (.getBytes string "UTF-8")))


(deftest read-non-data-blob
  (let [content "foobarbaz not a data blob"
        result (read-data (string-input content))]
    (is (instance? java.io.InputStream result))
    (is (= content (slurp result)))))


(deftest read-data-blob
  (let [content "#vault/data\n[:foo]"
        result (read-data (string-input content))]
    (is (= [:foo] result))))


(deftest read-data-blob-with-primary-bytes
  (binding [*primary-bytes* nil]
    (let [primary-content "#{:bar \"baz\"}"
          content (str "#vault/data\n" primary-content "\n\n:frobble")
          result (read-data (string-input content))]
      (is (not (nil? *primary-bytes*)))
      (is (= (seq (.getBytes primary-content "UTF-8")) (seq *primary-bytes*))))))
