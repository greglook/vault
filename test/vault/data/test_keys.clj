(ns vault.data.test-keys
  (:require
    [clojure.java.io :as io]
    [mvxcvi.crypto.pgp :as pgp]
    [vault.blob.core :as blob]
    [vault.blob.store.memory :refer [memory-store]]
    [vault.data.pgp :as pgp-data]
    [vault.data.crypto :as crypto]))


(def pubring
  (-> "vault/data/test_keys/pubring.gpg"
      io/resource
      io/file
      pgp/load-public-keyring))


(def secring
  (-> "vault/data/test_keys/secring.gpg"
      io/resource
      io/file
      pgp/load-secret-keyring))


(def blob-store (memory-store))


(def pubkey
  (pgp/get-public-key secring "923b1c1c4392318a"))


(def pubkey-id
  (->> pubkey
       pgp-data/key->blob
       (blob/put! blob-store)
       :id))


(def sig-provider
  (crypto/keyring-sig-provider
    :sha1 secring
    (constantly "test password")))
