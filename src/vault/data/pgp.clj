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



;; KEY FUNCTIONS

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


(defn load-secret-key
  "Loads a secret key from the given keyring file."
  [keyring-file id]
  (with-open [input (PGPUtil/getDecoderStream
                      (io/input-stream keyring-file))]
    (.getSecretKey
      (PGPSecretKeyRingCollection. input)
      (key-id id))))


(defn extract-private-key
  [^PGPSecretKey secret-key
   ^String passphrase]
  (.extractPrivateKey secret-key
    (-> (BcPGPDigestCalculatorProvider.)
        (BcPBESecretKeyDecryptorBuilder.)
        (.build (.toCharArray passphrase)))))



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



;; DEBUGGING HELPERS

(defn print-key-info
  "Prints information about the given key."
  [k]
  (let [pkey (public-key k)
        strength (.getBitStrength pkey)
        algorithm (or (algorithm-name
                        public-key-algorithms
                        (key-algorithm pkey))
                      "UNKNOWN")
        fingerprint (->> (.getFingerprint pkey)
                         (map (partial format "%02X"))
                         string/join)]
    (println (if (.isMasterKey pkey) "Master key:" "Subkey:")
             (Long/toHexString (key-id k))
             (str \( strength \/ algorithm \)))
    (println "    Fingerprint:"
             (str (subs fingerprint 0 (- (count fingerprint) 8)) \/
                  (subs fingerprint (- (count fingerprint) 8))))
    (if (.isEncryptionKey pkey)
      (println "    Public key is available for encryption.")
      (println "    Public key is available."))
    (when (instance? PGPSecretKey k)
      (if (.isPrivateKeyEmpty ^PGPSecretKey k)
        (println "    Private key is not available.")
        (if (.isSigningKey ^PGPSecretKey k)
          (println "    Private key is available for decryption and signing.")
          (println "    Private key is available for decryption."))))
    (doseq [uid (iterator-seq (.getUserIDs pkey))]
      (println "    User ID:" uid))))
