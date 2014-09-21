(ns vault.data.pgp-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.test :refer :all]
    [mvxcvi.crypto.pgp :as pgp]
    [vault.blob.core :as blob]
    (vault.data
      [pgp :as pgp-data]
      [test-keys :as keys])))


(def pubkey
  (pgp/get-public-key keys/secring "923b1c1c4392318a"))


(deftest pgp-blobs
  (let [blob (pgp-data/key->blob pubkey)]
    (is (= :pgp/public-key (:data/type blob)))
    (is (= [pubkey] (:data/values blob)))
    (let [raw-blob (select-keys blob [:id :content])
          parsed-blob (pgp-data/parse-blob raw-blob)]
      (is (= :pgp/public-key (:data/type blob)))
      (is (= [pubkey] (:data/values blob))))))
