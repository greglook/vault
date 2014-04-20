(ns vault.data.format-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.test :refer :all]
    [vault.data.format :as format]))


(defn data-fixture
  "Builds a string representing a data blob from the given sequence of values."
  [& values]
  (apply str "#vault/data\n" (interpose "\n\n" values)))



;; SERIALIZATION

(deftest print-data-blob
  (is (= "#vault/data\n{:alpha \"foo\", :omega \"bar\"}"
         (format/print-data-str {:omega "bar" :alpha "foo"})))
  (is (= "#vault/data\n[:foo \\b baz]\n\n{:name \"Aaron\"}\n\n:frobnitz"
         (format/print-data-str [:foo \b 'baz] {:name "Aaron"} :frobnitz))))



;; DESERIALIZATION

(deftest read-non-data-blob
  (let [content "foobarbaz not a data blob"
        result (format/read-data content)]
    (is (nil? result))))


(deftest read-data-blob
  (let [content (data-fixture "[:foo]")
        result (format/read-data content)]
    (is (= '([:foo]) result))))


(deftest read-custom-tag
  (let [content (data-fixture "{:foo #my/tag :bar}")
        result (format/read-data {'my/tag str} content)]
    (is (= '({:foo ":bar"}) result))))


(deftest read-primary-bytes
  (let [primary-content "[1 \\2 :three]"
        content (data-fixture primary-content)
        result (format/read-data content)
        primary-bytes (format/primary-bytes result)]
    (is (= '([1 \2 :three]) result))
    (is (not (nil? (meta result))))
    (is (bytes= (.getBytes primary-content format/blob-charset) primary-bytes))
    (is (bytes= primary-bytes (format/value-bytes (first result))))))


(deftest read-primary-bytes-with-extra-values
  (let [primary-content "#{\"baz\" :bar}"
        content (data-fixture primary-content ":frobble")
        result (format/read-data content)
        primary-bytes (format/primary-bytes result)]
    (is (= '(#{:bar "baz"} :frobble) result))
    (is (not (nil? (meta result))))
    (is (bytes= (.getBytes primary-content format/blob-charset) primary-bytes))
    (is (bytes= primary-bytes (format/value-bytes (first result))))))
