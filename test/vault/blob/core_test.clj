(ns vault.blob.core-test
  (:require
    [clojure.test :refer :all]
    [vault.blob.core :as blob :refer [BlobStore]])
  (:import
    vault.blob.core.HashID))


(deftest list-wrapper
  (let [store (reify BlobStore (-list [this opts] (vector :list opts)))]
    (is (= [:list nil] (blob/list store)))
    (is (= [:list {:foo "bar" :baz 3}] (blob/list store :foo "bar" :baz 3)))))


(deftest stat-wrapper
  (let [store (reify BlobStore (-stat [this id] (vector :stat id)))]
    (is (= [:stat :id] (blob/stat store :id)))))


(deftest contains?-wrapper
  (let [store (reify BlobStore (-stat [this id] nil))]
    (is (false? (blob/contains? store :id))))
  (let [store (reify BlobStore (-stat [this id] {:size 1}))]
    (is (true? (blob/contains? store :id)))))


(deftest open-wrapper
  (let [store (reify BlobStore (-open [this id] (vector :open id)))]
    (is (= [:open :id] (blob/open store :id)))))


(deftest store!-wrapper
  (let [store (reify BlobStore (-store! [this blob] (vector :store (str (:id blob)))))]
    (is (instance? HashID (blob/store! store "foobarbaz")))))


(deftest remove!-wrapper
  (let [store (reify BlobStore (-remove! [this id] (vector :remove id)))]
    (is (= [:remove :id] (blob/remove! store :id)))))
