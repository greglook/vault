(ns vault.index.engine.memory-test
  (:require
    [clojure.test :refer :all]
    [vault.index.core :as index]
    [vault.index.engine.memory :refer [memory-index]]))


(deftest memory-index-test
  (let [idx (memory-index {:foo [:alpha :beta :gamma]
                           :bar [:beta :omega :alpha]
                           :baz [:gamma]})
        r1 {:alpha 123, :beta "456", :gamma true,  :omega :foo}
        r2 {:alpha 123, :beta "789", :gamma false, :omega :bar}]
    (is (empty? (index/search idx {:alpha 123} nil)))
    (is (thrown? IllegalArgumentException
                 (index/update! idx {:alpha 123, :beta "456"})))
    (is (identical? idx (index/update! idx r1)))
    (index/update! idx r2)
    (is (= [r1 r2] (index/search idx {:alpha 123} nil)))
    (is (= [r2] (index/search idx {:gamma false} nil)))))
