(ns mvxcvi.pgp.core
  "Functions for interacting with BouncyCastle's OpenPGP library."
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [mvxcvi.pgp.util :refer [do-bytes hex-str]])
  (:import
    (java.io
      ByteArrayOutputStream)
    (org.bouncycastle.bcpg
      ArmoredOutputStream
      BCPGOutputStream
      HashAlgorithmTags
      PublicKeyAlgorithmTags)
    (org.bouncycastle.openpgp
      PGPObjectFactory
      PGPPrivateKey
      PGPPublicKey
      PGPPublicKeyRing
      PGPPublicKeyRingCollection
      PGPSecretKey
      PGPSecretKeyRing
      PGPSecretKeyRingCollection
      PGPSignature
      PGPSignatureGenerator
      PGPSignatureList
      PGPUtil)
    (org.bouncycastle.openpgp.operator.bc
      BcPGPContentSignerBuilder
      BcPGPContentVerifierBuilderProvider
      BcPGPDigestCalculatorProvider
      BcPBESecretKeyDecryptorBuilder)))


;; CONFIGURATION

(defn- map-tags
  "Converts static 'tag' fields on the given class into a map of keywords to
  numeric codes."
  [^Class tags]
  (let [field->entry
        (fn [^java.lang.reflect.Field f]
          (vector (-> (.getName f)
                      (string/replace \_ \-)
                      .toLowerCase
                      keyword)
                  (.getInt f nil)))]
    (->> (.getFields tags)
         (map field->entry)
         (into {}))))


(defn- code->name
  "Look up the keyword of an algorithm given the numeric code."
  [codes code]
  (some #(if (= (val %) code) (key %)) codes))


(def public-key-algorithms
  "Map of public-key algorithm names to numeric codes."
  (map-tags PublicKeyAlgorithmTags))


(def hash-algorithms
  "Map of hash algorithm keywords to numeric codes."
  (map-tags HashAlgorithmTags))


(def ^:dynamic *hash-algorithm*
  "Digest algorithm used for creating signatures."
  :sha1)



;; KEY UTILITIES

(defmulti ^PGPPublicKey public-key
  "Determines the public PGP key associated with the argument."
  class)

(defmethod public-key PGPPublicKey
  [^PGPPublicKey pubkey]
  pubkey)

(defmethod public-key PGPSecretKey
  [^PGPSecretKey seckey]
  (.getPublicKey seckey))


(defmulti key-id
  "Determines the numeric PGP key identifier from the argument."
  class)

(defmethod key-id nil [_] nil)

(defmethod key-id Long [id] id)

(defmethod key-id String
  [^String hex]
  (-> hex (BigInteger. 16) .longValue))

(defmethod key-id PGPPublicKey
  [^PGPPublicKey pubkey]
  (.getKeyID pubkey))

(defmethod key-id PGPSecretKey
  [^PGPSecretKey seckey]
  (.getKeyID seckey))

(defmethod key-id PGPPrivateKey
  [^PGPPrivateKey privkey]
  (.getKeyID privkey))

(defmethod key-id PGPSignature
  [^PGPSignature sig]
  (.getKeyID sig))


(defmulti key-algorithm
  "Constructs a numeric PGP key identifier from the argument."
  class)

(defmethod key-algorithm nil [_] nil)

(defmethod key-algorithm Number [code] (code->name public-key-algorithms code))

(defmethod key-algorithm clojure.lang.Keyword [kw] kw)

(defmethod key-algorithm PGPPublicKey
  [^PGPPublicKey pubkey]
  (key-algorithm (.getAlgorithm pubkey)))

(defmethod key-algorithm PGPSecretKey
  [^PGPSecretKey seckey]
  (key-algorithm (public-key seckey)))

(defmethod key-algorithm PGPPrivateKey
  [^PGPPrivateKey privkey]
  (key-algorithm (.getAlgorithm (.getPublicKeyPacket privkey))))


(defn key-info
  "Returns a map of information about the given key."
  [k]
  (let [pubkey (public-key k)
        info
        {:master-key? (.isMasterKey pubkey)
         :key-id (hex-str (key-id pubkey))
         :strength (.getBitStrength pubkey)
         :algorithm (key-algorithm pubkey)
         :fingerprint (->> (.getFingerprint pubkey)
                           (map (partial format "%02X"))
                           string/join)
         :encryption-key? (.isEncryptionKey pubkey)
         :user-ids (-> pubkey .getUserIDs iterator-seq vec)}]
    (if (instance? PGPSecretKey k)
      (if (.isPrivateKeyEmpty ^PGPSecretKey k)
        (assoc info :private-key? false)
        (merge info
               {:private-key? true
                :signing-key? (.isSigningKey ^PGPSecretKey k)}))
      info)))


(defn unlock-key
  "Decodes a secret key with a passphrase to obtain the private key."
  ^PGPPrivateKey
  [^PGPSecretKey seckey
   ^String passphrase]
  (.extractPrivateKey seckey
    (-> (BcPGPDigestCalculatorProvider.)
        (BcPBESecretKeyDecryptorBuilder.)
        (.build (.toCharArray passphrase)))))



;; SIGNATURE FUNCTIONS

(defn sign
  "Generates a PGPSignature from the given data and private key."
  ([data privkey]
   (sign data *hash-algorithm* privkey))
  (^PGPSignature
   [data
    hash-algo
    ^PGPPrivateKey privkey]
   (let [generator (PGPSignatureGenerator.
                     (BcPGPContentSignerBuilder.
                       (public-key-algorithms (key-algorithm privkey))
                       (hash-algorithms hash-algo)))]
     (.init generator PGPSignature/BINARY_DOCUMENT privkey)
     (do-bytes [[buf n] data]
       (.update generator buf 0 n))
     (.generate generator))))


(defn verify
  [data
   ^PGPSignature signature
   ^PGPPublicKey pubkey]
  (when-not (= (key-id signature) (key-id pubkey))
    (throw (IllegalArgumentException.
             (str "Signature key id "
                  (Long/toHexString (key-id signature))
                  " doesn't match public key id "
                  (Long/toHexString (key-id pubkey))))))
  (.init signature
         (BcPGPContentVerifierBuilderProvider.)
         pubkey)
  (do-bytes [[buf n] data]
    (.update signature buf 0 n))
  (.verify signature))



;; SERIALIZATION

(defn encode-public-key
  "Encodes a public key as ascii-armored text."
  [^PGPPublicKey pubkey]
  (let [buffer (ByteArrayOutputStream.)]
    (with-open [writer (-> buffer ArmoredOutputStream. BCPGOutputStream.)]
      (.writePacket writer (.getPublicKeyPacket pubkey)))
    (str buffer)))


(defn decode-public-key
  "Decodes a public key from the given string."
  [data]
  nil ; FIXME
  #_ (-> data load-public-keyrings flatten first))


(defn encode-signature
  [^PGPSignature sig]
  (.getEncoded sig))


(defn decode-signature
  ^PGPSignature
  [data]
  (with-open [stream (PGPUtil/getDecoderStream
                       (byte-streams/to-input-stream data))]
    (let [^PGPSignatureList sigs
          (.nextObject (PGPObjectFactory. stream))]
      (when-not (instance? PGPSignatureList sigs)
        (throw (IllegalArgumentException.
                 (str "Data did not contain a PGPSignatureList: " sigs))))
      (when-not (.isEmpty sigs)
        (.get sigs 0)))))



;; KEY PROVIDER PROTOCOL

(defprotocol KeyProvider
  "Protocol for obtaining PGP keys."

  (get-public-key [this id]
    "Loads a public key by id.")

  (get-secret-key [this id]
    "Loads a secret key by id.")

  (get-private-key [this id] [this id passphrase]
    "Loads a private key by id."))
