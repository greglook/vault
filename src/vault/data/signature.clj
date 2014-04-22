(ns vault.data.signature
  "Signature handling functions."
  (:require
    [puget.data :refer [TaggedValue]]
    [vault.blob.core :as blob]
    [vault.data.format.edn :as edn-data]
    [vault.data.format.pgp :as pgp-data]
    [mvxcvi.crypto.pgp :as pgp])
  (:import
    (org.bouncycastle.openpgp
      PGPPrivateKey
      PGPPublicKey
      PGPSecretKey
      PGPSignature)))


(defn blob-signer
  "Creates a function which signs a byte array with the PGP private key
  matching the public key identified by a hash-id. Returns a sequence
  containing the constructed signature map."
  [store privkeys pubkey-id]
  (fn [value-bytes]
    (let [pubkey-blob (->> pubkey-id
                           (blob/get store)
                           pgp-data/read-blob)]
      (when-not pubkey-blob
        (throw (IllegalStateException.
                 (str "No public key blob stored for " pubkey-id))))
      (when-not (= :pgp/public-key (:data/type pubkey-blob))
        (prn pubkey-blob)
        (throw (IllegalStateException.
                 (str "Blob " pubkey-id " is not a PGP public key"))))
      (let [pubkey (first (:data/values pubkey-blob))
            privkey (privkeys (pgp/key-id pubkey))
            pgp-sig (pgp/sign value-bytes privkey)]
        [{:key pubkey-id
          :signature pgp-sig
          :vault.data/type :vault/signature}]))))


(defn verify-blob
  "Verifies that the inline signatures in a sequence of blob values are valid."
  [blob-store
   blob-values]
  ; TODO: implement
  nil)
