(ns vault.data.print-test
  (:require [clojure.test :refer :all]
            [vault.data :as data]
            [vault.data.print :refer :all]))


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
      (is (thrown? IllegalArgumentException (edn-blob r)) "should not print canonical representation")
      (is (= (with-out-str (pprint r))
             "#vault.data.print_test.TestRecord{:bar \\y, :foo \\x}\n")))))


(deftest special-blob-format
  (let [tv (reify data/TaggedValue
             (tag [this] 'my-app/type)
             (value [this] {:alpha 'foo :omega 1234}))]
    (is (= (edn-blob tv)
           "#my-app/type\n{:alpha foo, :omega 1234}")
        "top-level tagged values in blob should print tag on separate line")))
