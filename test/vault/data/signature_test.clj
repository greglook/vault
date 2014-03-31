(ns vault.data.signature-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [mvxcvi.crypto.pgp :as pgp]
    [mvxcvi.crypto.pgp.keyring :as keyring]
    [mvxcvi.crypto.pgp.provider :refer :all]
    (puget
      [data]
      [printer :as puget])
    [vault.blob.core :as blob]
    [vault.blob.store.memory :refer [memory-store]]
    [vault.data.format :as fmt]
    [vault.data.signature :as sig])
  (:import
    (org.bouncycastle.openpgp
      PGPSignature)))


(puget.data/extend-tagged-value PGPSignature pgp/signature pgp/encode)

(def blob-store (memory-store))

(def test-keyring
  (keyring/pgp-keyring
    (io/resource "test-resources/pgp/pubring.gpg")
    (io/resource "test-resources/pgp/secring.gpg")))

(def pubkey
  (pgp/get-public-key test-keyring "923b1c1c4392318a"))

(def pubkey-hash
  (blob/put! blob-store (pgp/encode-ascii pubkey)))


(deftest signed-blob
  (let [value {:foo "bar", :baz :frobble, :alpha 12345}
        provider (-> test-keyring
                     keystore-provider
                     caching-provider)
        privkey (provider (pgp/key-id pubkey) "test password")]
    (is privkey "Private key should be unlocked")
    (let [sig (sig/sign-value blob-store provider value pubkey-hash)]
      (println "Signed blob:")
      (binding [puget/*colored-output* true]
        (fmt/print-data value sig)))))
