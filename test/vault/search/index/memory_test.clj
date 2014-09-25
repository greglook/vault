(ns vault.search.index.memory-test
  (:require
    [clojure.test :refer :all]
    [vault.search.index :as index]
    [vault.search.index.memory :refer [memory-index]]))


(deftest memory-index-test
  (let [idx (memory-index :alpha :omega)
        r1 {:alpha 123, :beta "456", :gamma true,  :omega :foo}
        r2 {:alpha 123, :beta "789", :gamma false, :omega :bar}]
    (is (empty? (index/seek idx)))
    (is (identical? idx (index/insert! idx r1)))
    (is (= [r1] (index/seek idx nil)))
    (is (empty? (index/seek idx {:alpha 200})))
    (index/insert! idx r2)
    ; TODO: more tests
    ))
