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
  [store privkeys data pubkey-id]
  (let [pubkey (load-pubkey store pubkey-id)
        privkey (privkeys (pgp/key-id pubkey))
        pgp-sig (pgp/sign data privkey)]
    {:key pubkey-id
     :signature pgp-sig
     :vault.data/type :vault/signature}))


(defn blob-signer
  "Creates a function which signs a byte array with the PGP private key
  matching the public key identified by a hash-id. Returns a sequence
  containing the constructed signature map."
  [store privkeys & pubkey-ids]
  (fn [data]
    (map (partial sign-bytes store privkeys data) pubkey-ids)))


(defn- inline-signatures
  "Collects the inline signatures from a data blob."
  [blob]
  (->>
    (rest (:data/values blob))
    (filter #(= :vault/signature (edn-data/data-type %)))
    seq))


(defn- verify-bytes
  [store data signature]
  (let [pubkey (load-pubkey store (:key signature))
        pgp-sig (:signature signature)]
    (if (pgp/verify data pgp-sig pubkey)
      (:key signature)
      ; TODO: log a warning about invalid signature
      )))


(defn verify
  "Verifies that the inline signatures in a blob are correct. Returns an
  updated blob record with the :data/signatures key giving a set of the public
  key hash ids of the blob signatures."
  [store blob]
  (if-let [signatures (inline-signatures blob)]
    (let [data (edn-data/primary-bytes blob)]
      (->>
        signatures
        (map (partial verify-bytes store data))
        (filter identity)
        set
        (assoc blob :data/signatures)))
    blob))
