(ns vault.data-test
  (:require [clojure.test :refer :all]
            [puget.data]
            [vault.data :refer :all]))


(deftest special-blob-format
  (let [tv (reify puget.data/TaggedValue
             (edn-tag [this] 'my-app/type)
             (edn-value [this] {:alpha 'foo :omega 1234}))]
    (is (= (edn-blob tv)
           "#my-app/type\n{:alpha foo, :omega 1234}")
        "top-level tagged values in blob should print tag on separate line")))
