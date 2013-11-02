(ns vault.data.print-test
  (:require [clojure.test :refer :all]
            [vault.data :as data]
            [vault.data.print :refer :all]))


(deftest canonical-primitives
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
       'ns/sym "ns/sym"))


(deftest canonical-collections
  (are [v text] (= text (edn-blob v))
       '(foo :bar)            "(foo :bar)"
       '(1 2 3)               "(1 2 3)"
       [4 "five" 6.0]         "[4 \"five\" 6.0]"
       {:foo 8 :bar 'baz}     "{:bar baz, :foo 8}" ; gets sorted
       #{:omega :alpha :beta} "#{:alpha :beta :omega}")) ; also sorted


; TODO: test that records and unknown types are rejected in strict mode
; TODO: test that top-level tagged values are newline-separated
