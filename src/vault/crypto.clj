(ns vault.crypto
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import
    (org.bouncycastle.openpgp
      PGPObjectFactory
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


;; CONSTANTS

(def ^:dynamic *hash-algorithm*
  "Digest algorithm used for creating signatures."
  "SHA1")


(def hash-algorithms
  "Map of numeric codes to available hash algorithm names."
  (->> (.getFields org.bouncycastle.bcpg.HashAlgorithmTags)
       (map #(vector (.getName %) (.getInt % nil)))
       (into {})))


(def public-key-algorithms
  "Map of numeric codes to available public-key algorithm names."
  (->> (.getFields org.bouncycastle.bcpg.PublicKeyAlgorithmTags)
       (map #(vector (.getName %) (.getInt % nil)))
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
        (instance? PGPPublicKey x) (.getKeyID x)
        (instance? PGPSecretKey x) (.getKeyID x)
        (instance? PGPSignature x) (.getKeyID x)
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to make key id from: " x)))))


(defn- key-algorithm-code
  "Returns the numeric key algorithm code from the argument."
  [k]
  (cond (instance? PGPPublicKey k) (.getAlgorithm k)
        (instance? PGPSecretKey k) (.getAlgorithm (.getPublicKey k))
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to get key algorithm from: " k)))))

(defn key-algorithm
  "Returns the name of the algorithm used by the given key."
  [k]
  (algorithm-name public-key-algorithms (key-algorithm-code k)))


(defn load-secret-key
  "Loads a secret key from the given keyring file."
  [keyring-file id]
  (with-open [input (PGPUtil/getDecoderStream
                      (io/input-stream keyring-file))]
    (-> (PGPSecretKeyRingCollection. input)
        (.getSecretKey (key-id id)))))


(defn- extract-private-key
  [secret-key passphrase]
  (.extractPrivateKey secret-key
    (-> (BcPGPDigestCalculatorProvider.)
        (BcPBESecretKeyDecryptorBuilder.)
        (.build (.toCharArray passphrase)))))



;; SIGNATURE FUNCTIONS

(defn- build-signature-generator
  [hash-algorithm-code key-algorithm-code]
  (PGPSignatureGenerator.
   (BcPGPContentSignerBuilder.
     key-algorithm-code
     hash-algorithm-code)))


(defn- generate-signature
  [data-source
   hash-algorithm-code
   key-algorithm-code
   private-key]
  (let [generator (build-signature-generator
                    hash-algorithm-code
                    key-algorithm-code)]
    (.init generator PGPSignature/BINARY_DOCUMENT private-key)
    (with-open [data (io/input-stream data-source)]
      (let [buffer (byte-array 1024)]
        (loop [n (.read data buffer)]
          (when (> n 0)
            (.update generator buffer 0 n)
            (recur (.read data buffer))))))
    (.generate generator)))


(defn sign-data
  [data-source secret-key passphrase]
  (generate-signature
    data-source
    (hash-algorithms *hash-algorithm*)
    (key-algorithm-code secret-key)
    (extract-private-key secret-key passphrase)))


(defn verify-signature
  [data-source signature public-key]
  (when-not (= (key-id signature) (key-id public-key))
    (throw (IllegalArgumentException.
             (str "Signature key id "
                  (Long/toHexString (key-id signature))
                  " doesn't match public key id "
                  (Long/toHexString (key-id public-key))))))
  (.init signature
         (BcPGPContentVerifierBuilderProvider.)
         public-key)
  (with-open [data (io/input-stream data-source)]
    (let [buffer (byte-array 1024)]
      (loop [n (.read data buffer)]
        (when (> n 0)
          (.update signature buffer 0 n)
          (recur (.read data buffer))))))
  (.verify signature))


(defn encode-signature
  [sig]
  (.getEncoded sig))


(defn decode-signature
  [data]
  (with-open [data-stream (PGPUtil/getDecoderStream
                            (io/input-stream data))]
    (let [sig-list (.nextObject (PGPObjectFactory. data-stream))]
      (when-not (instance? PGPSignatureList sig-list)
        (throw (IllegalArgumentException.
                 (str "Data did not contain a PGPSignatureList: " sig-list))))
      (when-not (.isEmpty sig-list)
        (.get sig-list 0)))))



;; DEBUGGING HELPERS

(defn print-key-info
  "Prints information about the given key."
  [k]
  (let [public-key (if (instance? PGPSecretKey k) (.getPublicKey k) k)
        strength (.getBitStrength public-key)
        algorithm (or (key-algorithm public-key) "UNKNOWN")
        fingerprint (->> (.getFingerprint public-key)
                         (map (partial format "%02X"))
                         string/join)]
    (println (if (.isMasterKey k) "Master key:" "Subkey:")
             (Long/toHexString (key-id k))
             (str \( strength \/ algorithm \)))
    (println "    Fingerprint:"
             (str (subs fingerprint 0 (- (count fingerprint) 8)) \/
                  (subs fingerprint (- (count fingerprint) 8))))
    (if (.isEncryptionKey public-key)
      (println "    Public key is available for encryption.")
      (println "    Public key is available."))
    (when (instance? PGPSecretKey k)
      (if (.isPrivateKeyEmpty k)
        (println "    Private key is not available.")
        (if (.isSigningKey k)
          (println "    Private key is available for decryption and signing.")
          (println "    Private key is available for decryption."))))
    (doseq [uid (iterator-seq (.getUserIDs public-key))]
      (println "    User ID:" uid))))
