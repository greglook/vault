(ns vault.blob.core-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.test :refer :all]
    [vault.blob.core :as blob :refer [BlobStore]])
  (:import
    vault.blob.core.HashID))


(deftest list-wrapper
  (let [store (reify BlobStore (enumerate [this opts] (vector :list opts)))]
    (is (= [:list nil] (blob/list store)))
    (is (= [:list {:foo "bar" :baz 3}] (blob/list store :foo "bar" :baz 3)))))


(deftest contains?-wrapper
  (let [store (reify BlobStore (stat [this id] nil))]
    (is (false? (blob/contains? store :id))))
  (let [store (reify BlobStore (stat [this id] {:size 1}))]
    (is (true? (blob/contains? store :id)))))


(deftest get-wrapper
  (let [content "foobarbaz"
        id (blob/hash :sha256 content)
        store (reify BlobStore (open [this id]
                                 (byte-streams/to-input-stream content)))
        blob (blob/get store id)]
    (is (= id (:id blob)))
    (is (bytes= content (:content blob)))
    (is (thrown? RuntimeException
                 (blob/get store (blob/hash :sha256 "bazbarfoo"))))))


(deftest put!-wrapper
  (let [store (reify BlobStore (store! [this blob] true))]
    (is (instance? HashID (blob/put! store "foobarbaz")))))
