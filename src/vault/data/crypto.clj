(ns vault.data.crypto
  "Cryptographic provider functions."
  (:require
    [mvxcvi.crypto.pgp :as pgp]
    [vault.blob.core :as blob]
    (vault.data
      [edn :as edn-data]
      [pgp :as pgp-data]))
  (:import
    (org.bouncycastle.openpgp
      PGPSignature)))


(edn-data/register-tag! pgp/signature
  PGPSignature pgp/encode
  pgp/decode-signature)



;; SIGNATURE PROVIDER PROTOCOL

; TODO: Support an eventual PKCS#11-style implementation which doesn't directly
; handle keys. Possible implementations include gpg-agent, gnome-keyring, OS X
; keychain, etc.

(defprotocol SignatureProvider
  "Protocol for cryptographic signature providers."

  (sign-content [this key-id content]
    "Returns a PGP signature of the given byte content with the identified
    private key."))



;; PRIVATE KEY PROVIDER

(defrecord PrivateKeySignatureProvider [hash-algorithm privkeys])


(extend-type PrivateKeySignatureProvider
  SignatureProvider

  (sign-content [this key-id content]
    (let [privkey ((:privkeys this) key-id)]
      (pgp/sign content (:hash-algorithm this) privkey))))


(defn privkey-signature-provider
  "A signing implementation which directly uses private keys to sign content.
  This must be provided with a `privkeys` function which maps numeric key-ids
  to an unlocked private key."
  [algorithm privkeys]
  {:pre [(fn? privkeys)]}
  (PrivateKeySignatureProvider. algorithm privkeys))



;; UTILITY FUNCTIONS

; Crypto functions generally need a blob store to load public keys:
; load-pubkey :: BlobStore -> HashID -> PGPPublicKey
(defn load-pubkey
  "Loads a PGP public key from a blob store."
  [store id]
  (let [blob (blob/get store id)
        pubkey-blob (pgp-data/read-blob blob)]
    (when-not blob
      (throw (IllegalStateException.
               (str "No public key blob stored for " id))))
    (when-not (= :pgp/public-key (:data/type pubkey-blob))
      (throw (IllegalStateException.
               (str "Blob " id " is not a PGP public key"))))
    (first (:data/values pubkey-blob))))



;; SIGNATURE CREATION

(defn- signature-map
  "Signs a byte array with a single public key."
  [store provider content pubkey-id]
  (let [pubkey (load-pubkey store pubkey-id)
        pgp-sig (sign-content provider (pgp/key-id pubkey) content)]
    (edn-data/typed-map
      :vault/signature
      :key pubkey-id
      :signature pgp-sig)))


(defn sign-value
  "Constructs a data blob with the given value, signed with the given public
  keys."
  [value store provider & pubkey-ids]
  (edn-data/edn-blob
    value
    (fn [content]
      (map (partial signature-map store provider content)
           pubkey-ids))))



;; SIGNATURE VERIFICATION

(defn- inline-signatures
  "Collects the inline signatures from a data blob."
  [blob]
  (->>
    (rest (:data/values blob))
    (filter #(= :vault/signature (edn-data/type %)))
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


(defn verify-sigs
  "Verifies that the inline signatures in a blob are correct. Returns an
  updated blob record with the :data/signatures key giving a set of the public
  key hash ids of the valid signatures."
  [blob store]
  (if-let [signatures (inline-signatures blob)]
    (let [data (edn-data/primary-bytes blob)]
      (->>
        signatures
        (map (partial verify-bytes store data))
        (filter identity)
        set
        (assoc blob :data/signatures)))
    blob))
