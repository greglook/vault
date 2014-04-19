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


(deftest get-wrapper
  (let [content "foobarbaz"
        id (blob/hash :sha256 content)
        store (reify BlobStore (get* [this id] (blob/load content)))
        blob (blob/get store id)]
    (is (= id (:id blob)))
    (is (bytes= content (:content blob)))
    (is (thrown? RuntimeException
                 (blob/get store (blob/hash :sha256 "bazbarfoo"))))))
