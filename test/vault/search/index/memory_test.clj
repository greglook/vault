(ns vault.search.index.memory-test
  (:require
    [clojure.test :refer :all]
    [vault.search.index :as index]
    [vault.search.index.memory :refer [memory-index]]))


(deftest memory-index-test
  (let [idx (memory-index :alpha :omega)
        r1 {:alpha 123, :beta "456", :gamma true,  :omega :bar}
        r2 {:alpha 123, :beta "789", :gamma false, :omega :foo}]
    (is (empty? (index/seek idx)))
    (is (identical? idx (index/insert! idx r1)))
    ; SortedIndex
    (is (= [r1] (index/seek idx nil)))
    (is (empty? (index/seek idx {:alpha 200})))
    (is (= [r1] (index/seek idx {:alpha 200} false)))
    (index/insert! idx r2)
    (is (= [r2]    (index/seek idx {:alpha 123, :omega :cat})))
    (is (= [r2 r1] (index/seek idx {:alpha 300} false)))
    (is (= [r1]    (index/seek idx nil {:alpha 123, :omega :cat} true)))
    (is (= [r2]    (index/seek idx {:alpha 500} {:alpha 123, :omega :dog} false)))
    ; KeyValueIndex
    (is (nil? (index/get idx {:alpha 123})))
    (is (= r1 (index/get idx {:alpha 123, :omega :bar})))
    (is (= r2 (index/get idx {:alpha 123, :omega :foo})))
    (index/delete! idx {:alpha 123, :omega :elk})
    (is (= 2 (count (index/seek idx))))
    (index/delete! idx {:alpha 123, :omega :foo})
    (is (nil? (index/get idx {:alpha 123, :omega :foo})))
    (index/erase!! idx)
    (is (empty? (index/seek idx)))))
