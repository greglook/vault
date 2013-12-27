(ns vault.data.pgp
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [clojure.string :as string])
  (:import
    (org.bouncycastle.bcpg
      HashAlgorithmTags
      PublicKeyAlgorithmTags)
    (org.bouncycastle.openpgp
      PGPObjectFactory
      PGPPrivateKey
      PGPPublicKey
      PGPPublicKeyRingCollection
      PGPSecretKey
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

(def ^:dynamic *hash-algorithm*
  "Digest algorithm used for creating signatures."
  "SHA1")


(def hash-algorithms
  "Map of hash algorithm names to numeric codes."
  (->> (.getFields HashAlgorithmTags)
       (map (fn [^java.lang.reflect.Field f]
              (vector (.getName f)
                      (.getInt f nil))))
       (into {})))


(def public-key-algorithms
  "Map of public-key algorithm names to numeric codes."
  (->> (.getFields PublicKeyAlgorithmTags)
       (map (fn [^java.lang.reflect.Field f]
              (vector (.getName f)
                      (.getInt f nil))))
       (into {})))


(defn- algorithm-name
  "Look up the name of an algorithm given the numeric code."
  [codes code]
  (some (fn [[k i]] (if (= i code) k)) codes))



;; KEY UTILITIES

(defn key-id
  "Returns the PGP key identifier associated with the argument."
  [x]
  (cond (integer? x) x
        (string? x) (Long/parseLong x 16)
        (instance? PGPPublicKey x) (.getKeyID ^PGPPublicKey x)
        (instance? PGPSecretKey x) (.getKeyID ^PGPSecretKey x)
        (instance? PGPSignature x) (.getKeyID ^PGPSignature x)
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to make key id from: " x)))))


(defn key-algorithm
  "Returns the numeric key algorithm code from the argument."
  [k]
  (cond (instance? PGPPublicKey k) (.getAlgorithm ^PGPPublicKey k)
        (instance? PGPSecretKey k) (.getAlgorithm (.getPublicKey ^PGPSecretKey k))
        (instance? PGPPrivateKey k) (.getAlgorithm (.getPublicKeyPacket ^PGPPrivateKey k))
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to get key algorithm from: " k)))))


(defn public-key
  "Coerces the argument into a PGPPublicKey."
  ^PGPPublicKey
  [k]
  (cond (instance? PGPPublicKey k) k
        (instance? PGPSecretKey k) (.getPublicKey ^PGPSecretKey k)
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to get public key from: " k)))))


(defn extract-private-key
  [^PGPSecretKey secret-key
   ^String passphrase]
  (.extractPrivateKey secret-key
    (-> (BcPGPDigestCalculatorProvider.)
        (BcPBESecretKeyDecryptorBuilder.)
        (.build (.toCharArray passphrase)))))


(defn key-info
  "Returns a map of information about the given key."
  [k]
  (let [pubkey (public-key k)
        info
        {:master-key? (.isMasterKey pubkey)
         :key-id (Long/toHexString (key-id pubkey))
         :strength (.getBitStrength pubkey)
         :algorithm (algorithm-name public-key-algorithms (key-algorithm pubkey))
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



;; KEYRING FUNCTIONS

(defn load-secret-keyrings
  "Loads a secret keyring file into a sequence of vectors of secret keys."
  [source]
  (with-open [stream (PGPUtil/getDecoderStream
                       (byte-streams/to-input-stream source))]
    (map #(-> (.getSecretKeys %)
              iterator-seq
              vec)
          (-> stream
              PGPSecretKeyRingCollection.
              .getKeyRings
              iterator-seq))))



;; SIGNATURE FUNCTIONS

(defn for-bytes
  "Calls the given function on chunks of the byte sequence read from the given
  data source."
  [source f]
  (with-open [stream (byte-streams/to-input-stream source)]
    (let [buffer (byte-array 1024)]
      (loop [n (.read stream buffer)]
        (when (pos? n)
          (f buffer n)
          (recur (.read stream buffer)))))))


(defn sign-data
  "Generates a PGPSignature from the given data and private key."
  ([data-source private-key]
   (sign-data data-source
              (hash-algorithms *hash-algorithm*)
              private-key))
  ([data-source hash-algo private-key]
   (let [generator (PGPSignatureGenerator.
                     (BcPGPContentSignerBuilder.
                       (key-algorithm private-key)
                       hash-algo))]
     (.init generator PGPSignature/BINARY_DOCUMENT private-key)
     (for-bytes data-source #(.update generator %1 0 %2))
     (.generate generator))))


(defn verify-signature
  [data-source
   ^PGPSignature signature
   ^PGPPublicKey public-key]
  (when-not (= (key-id signature) (key-id public-key))
    (throw (IllegalArgumentException.
             (str "Signature key id "
                  (Long/toHexString (key-id signature))
                  " doesn't match public key id "
                  (Long/toHexString (key-id public-key))))))
  (.init signature
         (BcPGPContentVerifierBuilderProvider.)
         public-key)
  (for-bytes data-source #(.update signature %1 0 %2))
  (.verify signature))


(defn encode-signature
  [^PGPSignature sig]
  (.getEncoded sig))


(defn decode-signature
  [data]
  (with-open [stream (PGPUtil/getDecoderStream
                       (byte-streams/to-input-stream data))]
    (let [sigs (.nextObject (PGPObjectFactory. stream))]
      (when-not (instance? PGPSignatureList sigs)
        (throw (IllegalArgumentException.
                 (str "Data did not contain a PGPSignatureList: " sigs))))
      (when-not (.isEmpty ^PGPSignatureList sigs)
        (.get ^PGPSignatureList sigs 0)))))
