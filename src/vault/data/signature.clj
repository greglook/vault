(ns vault.data.signature
  "Cryptographic signature functions and provider protocol."
  (:require
    [clojure.java.io :as io]
    [mvxcvi.crypto.pgp :as pgp]
    [vault.blob.store :as store]
    [vault.data.edn :as edn]
    [vault.data.key :as key]
    [vault.data.struct :as struct])
  (:import
    org.bouncycastle.openpgp.PGPSignature))


;; Register an EDN tag for PGP signatures, encoded as binary blocks.
(edn/register-tag! pgp/signature
  PGPSignature pgp/encode
  pgp/decode-signature)


(defn load-pubkey
  "Loads a PGP public key from a blob store."
  [store id]
  (let [blob (store/get store id)
        pubkey-blob (key/parse-key blob)]
    (when-not blob
      (throw (IllegalStateException.
               (str "No public key blob stored for " id))))
    (when-not (= :pgp/public-key (struct/data-type pubkey-blob))
      (throw (IllegalStateException.
               (str "Blob " id " is not a PGP public key: "
                    (struct/data-type pubkey-blob)))))
    (struct/data-value pubkey-blob)))



;; ## Signature Creation

; TODO: Support an eventual PKCS#11-style implementation which doesn't directly
; handle keys. Possible implementations include gpg-agent, gnome-keyring, OS X
; keychain, etc.

(defprotocol SignatureProvider
  "Protocol for cryptographic signature providers."

  (sign-content
    [provider key-id content]
    "Returns a PGP signature of the given byte content with the identified
    private key."))


(defn- signature-map
  "Signs a byte array with a single public key."
  [store provider content pubkey-id]
  (let [pubkey (load-pubkey store pubkey-id)
        pgp-sig (sign-content provider (pgp/key-id pubkey) content)]
    (edn/typed-map
      :vault/signature
      :key pubkey-id
      :signature pgp-sig)))


(defn sign-value
  "Constructs a data blob with the given value, signed with the given public
  keys."
  [value store provider & pubkey-ids]
  (edn/data->blob value
    (fn [content]
      (map (partial signature-map store provider content)
           pubkey-ids))))



;; ## Signature Verification

(defn- inline-signatures
  "Collects the inline signatures from a data blob."
  [blob]
  (->>
    (rest (:data/values blob))
    (filter #(= :vault/signature (edn/value-type %)))
    seq))


(defn- verify-bytes
  "Verifies a signature map against the provided byte data by looking up public
  keys from the given blob store. Returns the hash-id of the public key if the
  signature is correct, otherwise nil."
  [store data signature]
  (let [pubkey (load-pubkey store (:key signature))
        pgp-sig (:signature signature)]
    (when (pgp/verify data pgp-sig pubkey)
      (:key signature))))


(defn verify-sigs
  "Verifies that the inline signatures in a blob are correct. Returns an
  updated blob record with the :data/signatures key giving a set of the public
  key hash ids of the valid signatures."
  [blob store]
  (if-let [signatures (inline-signatures blob)]
    (let [data (edn/primary-bytes blob)]
      (->>
        signatures
        (map (partial verify-bytes store data))
        (remove nil?)
        set
        (assoc blob :data/signatures)))
    blob))



;; ## Keyring Signatures

;; A signature provider which uses private keys from a secret keyring to sign
;; content. The `ask-pass` function should return a passphrase for a numeric PGP
;; key identiier. The provider will cache unlocked private keys so the
;; passphrase will not be asked more than once per key.
(defrecord KeyringSignatureProvider
  [secring privkeys hash-algorithm ask-pass]

  SignatureProvider

  (sign-content
    [this key-id content]
    (let [unlock-pk #(some-> secring
                             (pgp/get-secret-key %)
                             (pgp/unlock-key (ask-pass %)))
          privkey (or (get @privkeys key-id)
                      (when-let [pk (unlock-pk key-id)]
                        (swap! privkeys assoc key-id pk)
                        pk))]
      (pgp/sign content hash-algorithm privkey))))


(defn keyring-sig-provider
  "Constructs a new keyring signature provider."
  [hash-algorithm secring ask-pass]
  {:pre [(keyword? hash-algorithm) (some? secring) (fn? ask-pass)]}
  (KeyringSignatureProvider.
    secring
    (atom {})
    hash-algorithm
    ask-pass))
