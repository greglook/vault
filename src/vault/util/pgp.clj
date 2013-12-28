(ns vault.util.pgp
  "Utility functions for interacting with BouncyCastle's OpenPGP
  library interface."
  (:require
    [byte-streams]
    [clojure.string :as string]
    [vault.util.io :refer [do-bytes]])
  (:import
    (org.bouncycastle.bcpg
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


(def public-key-algorithms
  "Map of public-key algorithm names to numeric codes."
  (map-tags PublicKeyAlgorithmTags))


(def hash-algorithms
  "Map of hash algorithm keywords to numeric codes."
  (map-tags HashAlgorithmTags))


(def ^:dynamic *hash-algorithm*
  "Digest algorithm used for creating signatures."
  :sha1)


(defn- code->name
  "Look up the keyword of an algorithm given the numeric code."
  [codes code]
  (some #(if (= (val %) code) (key %)) codes))



;; KEY UTILITIES

(defn public-key
  "Coerces the argument into a PGPPublicKey."
  ^PGPPublicKey
  [k]
  (cond (instance? PGPPublicKey k) k
        (instance? PGPSecretKey k) (.getPublicKey ^PGPSecretKey k)
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to get public key from: " k)))))


(defn unlock-key
  "Decodes a secret key with a passphrase to obtain the private key."
  ^PGPPrivateKey
  [^PGPSecretKey seckey
   ^String passphrase]
  (.extractPrivateKey seckey
    (-> (BcPGPDigestCalculatorProvider.)
        (BcPBESecretKeyDecryptorBuilder.)
        (.build (.toCharArray passphrase)))))


(defmulti key-id
  "Constructs a numeric PGP key identifier from the argument."
  class)

(defmethod key-id nil [_] nil)

(defmethod key-id Long [id] id)

(defmethod key-id String [hex] (Long/parseLong hex 16))

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
         :key-id (Long/toHexString (key-id pubkey))
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


(defn find-key
  "Locates a key in a sequence by id. Nested sequences are flattened, so this
  works directly on keyrings and keyring collections."
  [id key-seq]
  (let [id (key-id id)]
    (some #(when (= id (key-id %)) %)
          (flatten key-seq))))



;; KEYRING FUNCTIONS

(defn load-secret-keyrings
  "Loads a secret keyring file into a sequence of vectors of secret keys."
  [source]
  (with-open [stream (PGPUtil/getDecoderStream
                       (byte-streams/to-input-stream source))]
    (map (fn [^PGPSecretKeyRing keyring]
           (vec (iterator-seq (.getSecretKeys keyring))))
         (-> stream
             PGPSecretKeyRingCollection.
             .getKeyRings
             iterator-seq))))



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
     (do-bytes data #(.update generator %1 0 %2))
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
  (do-bytes data #(.update signature %1 0 %2))
  (.verify signature))


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
