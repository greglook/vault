(ns vault.format.signature-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [mvxcvi.crypto.pgp :as pgp]
    (puget
      [data]
      [printer :as puget])
    [vault.blob.core :as blob]
    [vault.blob.store.memory :refer [memory-store]]
    [vault.format.edn :as edn-data]
    [vault.format.signature :as sig])
  (:import
    ; FIXME: why is this necessary??
    ; clojure.lang.Compiler$HostExpr.tagToClass(Compiler.java:1060)
    (org.bouncycastle.openpgp
      PGPPrivateKey
      PGPSecretKey)))


(def blob-store (memory-store))

(def test-keyring
  (pgp/load-secret-keyring
    (io/file (io/resource "test-resources/pgp/secring.gpg"))))

(def pubkey
  (pgp/get-public-key test-keyring "923b1c1c4392318a"))

(def pubkey-id
  (->> pubkey
       pgp/encode-ascii
       (blob/store! blob-store)
       :id))


(deftest signed-blob
  (let [value {:foo "bar", :baz :frobble, :alpha 12345}
        privkeys #(some-> test-keyring
                          (pgp/get-secret-key %)
                          (pgp/unlock-key "test password"))]
    (let [blob (->> pubkey-id
                    (sig/blob-signer blob-store privkeys)
                    (edn-data/edn-blob value)
                    (sig/verify blob-store))]
      #_
      (binding [puget/*colored-output* true]
        (println "Signed blob:")
        (edn-data/print-blob blob)
        (newline) (newline)
        (puget/pprint blob)))))
