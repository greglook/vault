(ns vault.data.edn-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.test :refer :all]
    [vault.blob.core :as blob]
    [vault.data.edn :as edn-data]))


; FIXME: This is necessary for some reason to placate Cloverage...
(edn-data/register-tag! vault/ref
  vault.blob.digest.HashID str
  blob/parse-id)


(defn data-fixture
  "Builds a string representing a data blob from the given sequence of values."
  [& values]
  (->> values
       (interpose "\n\n")
       (apply str "#vault/data\n")
       blob/load))


(deftest data-typing
  (is (= String (edn-data/type "foo")))
  (is (= (class :bar) (edn-data/type :bar)))
  (is (= :map (edn-data/type {:x 'y})))
  (is (= :set (edn-data/type #{:foo :bar})))
  (is (= :vector (edn-data/type [:foo :bar])))
  (is (= :test (edn-data/type {:vault/type :test}))))



;; SERIALIZATION

(deftest blob-creation
  (let [blob (edn-data/edn-blob [:foo])]
    (is (:id blob))
    (is (:content blob))
    (is (= :vector (:data/type blob)))
    (is (= [[:foo]] (:data/values blob)))
    (is (= [12 18] (:data/primary-bytes blob)))
    (is (= "[:foo]" (String. (edn-data/primary-bytes blob)))))
  (let [blob (edn-data/edn-blob {:alpha 'omega} (comp vector count))]
    (is (= [{:alpha 'omega} 14] (:data/values blob)))
    (is (= [12 26] (:data/primary-bytes blob)))))


(deftest blob-printing
  (is (= "#vault/data\n{:alpha \"foo\" :omega \"bar\"}\n"
         (with-out-str
           (edn-data/print-blob {:data/values [{:omega "bar" :alpha "foo"}]}))))
  (is (= "#vault/data\n[:foo \\b baz]\n\n{:name \"Aaron\"}\n\n:frobnitz\n"
         (with-out-str
           (edn-data/print-blob {:data/values [[:foo \b 'baz] {:name "Aaron"} :frobnitz]})))))



;; DESERIALIZATION

(deftest read-non-edn-blob
  (let [blob (blob/load "foobarbaz not a data blob")
        data (edn-data/read-blob blob)]
    (is (nil? data))))


(deftest read-data-blob
  (let [blob (data-fixture "{:foo bar, :vault/type :x/y}")
        data (edn-data/read-blob blob)]
    (is (= [{:foo 'bar, :vault/type :x/y}]
           (:data/values data)))
    (is (= :x/y (:data/type data)))))


(deftest read-primary-bytes
  (testing "data blob"
    (let [primary-value "[1 \\2 :three]"
          value-str (str primary-value " :x/y \"foo\"")
          blob (data-fixture value-str)
          data (edn-data/read-blob blob)
          values (:data/values data)
          primary-bytes (edn-data/primary-bytes data)]
      (is (= [[1 \2 :three] :x/y "foo"] values))
      (is (bytes= (.getBytes primary-value edn-data/blob-charset) primary-bytes))))
  (testing "non-data blob"
    (let [blob (blob/load "frobble babble")]
      (is (bytes= (:content blob) (edn-data/primary-bytes blob))))))


(deftest read-utf8-primary-bytes
  (let [value-str "\"€18.50\""
        blob (data-fixture value-str)
        data (edn-data/read-blob blob)]
    (is (bytes= (.getBytes value-str edn-data/blob-charset)
                (edn-data/primary-bytes data)))))
