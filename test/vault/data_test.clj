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
