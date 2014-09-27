(ns vault.data.key-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.test :refer :all]
    [mvxcvi.crypto.pgp :as pgp]
    (vault.data
      [key :as key]
      [test-keys :as test-keys])))


(def pubkey
  (pgp/get-public-key test-keys/secring "923b1c1c4392318a"))


(deftest pgp-blobs
  (let [blob (key/key->blob pubkey)]
    (is (= :pgp/public-key (:data/type blob)))
    (is (= [pubkey] (:data/values blob)))
    (let [raw-blob (select-keys blob [:id :content])
          parsed-blob (key/parse-key raw-blob)]
      (is (= :pgp/public-key (:data/type blob)))
      (is (= [pubkey] (:data/values blob))))))
