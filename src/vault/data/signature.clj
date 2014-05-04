(ns vault.data.signature
  "Signature handling functions."
  (:require
    [mvxcvi.crypto.pgp :as pgp]
    [vault.blob.core :as blob]
    (vault.data
      [edn :as edn-data]
      [pgp :as pgp-data]))
  (:import
    (org.bouncycastle.openpgp
      PGPSignature)))


;; UTILITY FUNCTIONS

(edn-data/register-tag! 'pgp/signature
  PGPSignature pgp/encode
  pgp/decode-signature)


(defn- load-pubkey
  "Loads a PGP public key from a blob store."
  [store id]
  (let [blob (blob/get store id)
        pubkey-blob (pgp-data/read-blob blob)]
    (when-not blob
      (throw (IllegalStateException.
               (str "No public key blob stored for " id))))
    (when (not= :pgp/public-key (:data/type pubkey-blob))
      (throw (IllegalStateException.
               (str "Blob " id " is not a PGP public key"))))
    (first (:data/values pubkey-blob))))



;; SIGNATURE CREATION

(def ^:dynamic *hash-algorithm*
  "Cryptographic hash algorithm to use for signature generation."
  :sha1)


(defn- sign-bytes
  "Signs a byte array with a single public key."
  [store privkeys data pubkey-id]
  (let [pubkey (load-pubkey store pubkey-id)
        privkey (privkeys (pgp/key-id pubkey))
        pgp-sig (pgp/sign data *hash-algorithm* privkey)]
    (assoc
      (edn-data/typed-map :vault/signature)
      :key pubkey-id
      :signature pgp-sig)))


(defn blob-signer
  "Creates a function which signs a byte array with the PGP private key
  matching the public key identified by a hash-id. Returns a sequence
  containing the constructed signature map."
  [store privkeys & pubkey-ids]
  (fn [data]
    (map (partial sign-bytes store privkeys data) pubkey-ids)))



;; SIGNATURE VERIFICATION

(defn- inline-signatures
  "Collects the inline signatures from a data blob."
  [blob]
  (->>
    (rest (:data/values blob))
    (filter #(= :vault/signature (edn-data/data-type %)))
    seq))


(defn- verify-bytes
  "Verifies a signature map against the provided byte data by looking up public
  keys from the given blob store. Returns the hash-id of the public key if the
  signature is correct, otherwise nil."
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
