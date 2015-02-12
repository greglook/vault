(ns vault.search.index-test
  (:require
    [clojure.test :refer :all]
    [vault.search.index :as index]
    [vault.search.index.memory :refer [memory-index]]))


(defn test-index
  "Tests an implementation of `vault.search.index/SortedIndex`."
  [label index]
  (let [r1 {:alpha 123, :beta "456", :gamma true,  :omega :bar}
        r2 {:alpha 123, :beta "789", :gamma false, :omega :foo}]
    (testing (.getSimpleName (class index))
      (println "  *" label)
      (is (empty? (index/seek index nil)))
      (is (identical? index (index/insert! index r1)))
      (is (= [r1] (index/seek index nil)))
      (is (empty? (index/seek index {:alpha 200})))
      (is (= [r1] (index/seek index {:alpha 123})))
      (index/insert! index r2)
      (is (= [r1 r2] (index/seek index {:alpha 123})))
      (is (= 2 (count (index/seek index nil))))
      (is (empty? (index/seek index {:alpha 123, :omega :zag})))
      (index/erase!! index)
      (is (empty? (index/seek index nil))))))


(deftest memory-index-test
  (let [idx (memory-index {:unique-key [:alpha :omega]})]
    (test-index "memory-index" idx)))
