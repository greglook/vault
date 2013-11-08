(ns vault.print-test
  (:require [clojure.test :refer :all]
            [vault.data :as data]
            [vault.print :refer :all]))


(deftest canonical-primitives
  (testing "Primitive values"
    (are [v text] (= text (edn-blob v))
         nil     "nil"
         true    "true"
         false   "false"
         0       "0"
         1234N   "1234N"
         2.718   "2.718"
         3.14M   "3.14M"
         3/10    "3/10"
         \a      "\\a"
         \space  "\\space"
         "foo"   "\"foo\""
         :key    ":key"
         :ns/key ":ns/key"
         'sym    "sym"
         'ns/sym "ns/sym")))


(deftest canonical-collections
  (testing "Collections"
    (are [v text] (= text (edn-blob v))
         '(foo :bar)            "(foo :bar)"
         '(1 2 3)               "(1 2 3)"
         [4 "five" 6.0]         "[4 \"five\" 6.0]"
         {:foo 8 :bar 'baz}     "{:bar baz, :foo 8}" ; gets sorted
         #{:omega :alpha :beta} "#{:alpha :beta :omega}"))) ; also sorted


(defrecord TestRecord [foo bar])

(deftest canonical-records
  (testing "Records"
    (let [r (->TestRecord \x \y)]
      (is (thrown? IllegalArgumentException (edn-blob r))
          "should not print non-EDN representation")
      (is (= (with-out-str (pprint r))
             "#vault.print_test.TestRecord{:bar \\y, :foo \\x}\n")))))


(deftest default-canonize
  (testing "Unknown values"
    (let [usd (java.util.Currency/getInstance "USD")]
      (is (thrown? IllegalArgumentException
                   (edn-blob usd))
                   "should not print non-EDN representation")
      (is (= (with-out-str (pprint usd))
             "#<java.util.Currency USD>\n")))))


(deftest special-blob-format
  (let [tv (reify data/TaggedValue
             (edn-tag [this] 'my-app/type)
             (edn-value [this] {:alpha 'foo :omega 1234}))]
    (is (= (edn-blob tv)
           "#my-app/type\n{:alpha foo, :omega 1234}")
        "top-level tagged values in blob should print tag on separate line")))
