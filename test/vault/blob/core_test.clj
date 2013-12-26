(ns vault.blob.core-test
  (:require
    [clojure.test :refer :all]
    [vault.blob.core :as blob :refer [BlobStore]]))


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
    (is (= [:store "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"]
           (blob/store! store "foobarbaz")))))


(deftest remove!-wrapper
  (let [store (reify BlobStore (-remove! [this id] (vector :remove id)))]
    (is (= [:remove :id] (blob/remove! store :id)))))
