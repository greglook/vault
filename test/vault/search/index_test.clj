(ns vault.search.index-test
  (:require
    [clojure.test :refer :all]
    [environ.core :refer [env]]
    [vault.search.index :as index]
    (vault.search.index
      [memory :refer [memory-index]])))


(defn test-index
  "Tests an implementation of `vault.search.index/Index`."
  [label index]
  (let [r1 {:alpha 123, :beta "456", :gamma true,  :omega :bar}
        r2 {:alpha 123, :beta "789", :gamma false, :omega :foo}]
    (testing (str (-> index class .getSimpleName))
      (println "  *" label)
      (is (empty? (index/search index)))
      (is (identical? index (index/insert! index r1)))
      (is (= [r1] (index/search index nil)))
      (is (empty? (index/search index {:where {:alpha 200}})))
      (is (= [r1] (index/search index {:where {:alpha 123}})))
      (index/insert! index r2)
      (is (= [r1 r2] (index/search index {:alpha 123})))
      (index/delete! index {:alpha 123, :omega :elk})
      (is (= 2 (count (index/search index))))
      (index/delete! index {:alpha 123, :omega :foo})
      (is (empty? (index/search index :where {:alpha 123, :omega :foo})))
      (index/erase!! index)
      (is (empty? (index/search index))))))


(deftest memory-index-test
  (let [idx (memory-index {:unique-key [:alpha :omega]})]
    (test-index "memory-index" idx)))
