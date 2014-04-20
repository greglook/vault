(ns vault.data.signature
  "Signature handling functions."
  (:require
    [puget.data :refer [TaggedValue]]
    [vault.blob.core :as blob]
    [vault.data.format.edn :as edn-blob]
    [mvxcvi.crypto.pgp :as pgp])
  (:import
    (org.bouncycastle.openpgp
      PGPPrivateKey
      PGPPublicKey
      PGPSecretKey
      PGPSignature)))


;; XXXX

(defn- load-public-key
  "Loads a public key from a blob-store by its hash-id."
  [store hash-id]
  (let [encoded-pubkey (:content (blob/get store hash-id))]
    (when-not encoded-pubkey
      (throw (IllegalStateException.
               (str "No public key blob stored for " hash-id))))
    (let [pubkey (pgp/decode-public-key encoded-pubkey)]
      (when-not (instance? PGPPublicKey pubkey)
        (throw (IllegalStateException.
                 (str "Blob " hash-id " is not a PGP public key"))))
      pubkey)))


(defn sign-value
  "Signs a clojure value with the PGP private key matching a public key
  identified by a hash-id. Returns the constructed signature map."
  [blob-store key-provider value pubkey-hash]
  (let [pubkey (load-public-key blob-store pubkey-hash)
        privkey (key-provider (pgp/key-id pubkey))
        value-bytes (edn-blob/value-bytes value)
        pgp-sig (pgp/sign value-bytes privkey)]
    {:key pubkey-hash
     :signature pgp-sig
     :vault/type :vault/signature}))


(defn verify-blob
  "Verifies that the inline signatures in a sequence of blob values are valid."
  [blob-store
   blob-values]
  ; TODO: implement
  nil)
