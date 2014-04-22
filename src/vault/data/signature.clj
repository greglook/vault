(ns vault.data.signature
  "Signature handling functions."
  (:require
    [puget.data :refer [TaggedValue]]
    [vault.blob.core :as blob]
    [vault.data.format.pgp :as pgp-data]
    [mvxcvi.crypto.pgp :as pgp])
  (:import
    (org.bouncycastle.openpgp
      PGPPublicKey
      PGPSignature)))


(defn- load-pubkey
  "Loads a PGP public key from a blob store."
  [store pubkey-id]
  (let [pubkey-blob (->> pubkey-id
                         (blob/get store)
                         pgp-data/read-blob)]
    (when-not pubkey-blob
      (throw (IllegalStateException.
               (str "No public key blob stored for " pubkey-id))))
    (when-not (= :pgp/public-key (:data/type pubkey-blob))
      (throw (IllegalStateException.
               (str "Blob " pubkey-id " is not a PGP public key"))))
    (first (:data/values pubkey-blob))))


(defn- sign-bytes
  "Signs a byte array with a single public key."
  [value-bytes store privkeys pubkey-id]
  (let [pubkey (load-pubkey store pubkey-id)
        privkey (privkeys (pgp/key-id pubkey))
        pgp-sig (pgp/sign value-bytes privkey)]
    {:key pubkey-id
     :signature pgp-sig
     :vault.data/type :vault/signature}))


(defn blob-signer
  "Creates a function which signs a byte array with the PGP private key
  matching the public key identified by a hash-id. Returns a sequence
  containing the constructed signature map."
  [store privkeys & pubkey-ids]
  (fn [value-bytes]
    (map (partial sign-bytes value-bytes store privkeys) pubkey-ids)))


(defn verify-blob
  "Verifies that the inline signatures in a sequence of blob values are valid."
  [blob-store
   blob-values]
  ; TODO: implement
  nil)
