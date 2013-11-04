(ns vault.crypto
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import
    (java.security Security)
    (org.bouncycastle.jce.provider BouncyCastleProvider)
    (org.bouncycastle.openpgp PGPUtil
                              PGPPublicKey
                              PGPPublicKeyRingCollection
                              PGPSecretKey
                              PGPSecretKeyRingCollection
                              PGPSignature
                              PGPSignatureGenerator
                              )))


; Install the BouncyCastle security provider.
(Security/addProvider (new BouncyCastleProvider))



;; CONSTANTS

(def ^:private security-provider "BC")


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



;; TEST FUNCTIONALITY

(defn key-id
  "Coerce argument into a PGP key identifier."
  [x]
  (cond (integer? x) x
        (string? x) (Long/parseLong x 16)
        (instance? PGPPublicKey x) (.getKeyID x)
        (instance? PGPSecretKey x) (.getKeyID x)
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to make key id from: " x)))))


(defn- key-algorithm-code
  "Returns numeric algorithm code from key argument."
  [k]
  (cond (instance? PGPPublicKey k) (.getAlgorithm k)
        (instance? PGPSecretKey k) (.getAlgorithm (.getPublicKey k))
        :else (throw (IllegalArgumentException.
                       (str "Don't know how to get key algorithm from: " k)))))

(defn key-algorithm
  "Returns the name of the algorithm used by the given key."
  [k]
  (algorithm-name public-key-algorithms (key-algorithm-code k)))


(defn signing-key? [k]
  (.isSigningKey k))


(defn load-secret-key
  "Loads a secret key from the given keyring file."
  [keyring-file id]
  (with-open [input (PGPUtil/getDecoderStream
                      (io/input-stream keyring-file))]
    (-> (PGPSecretKeyRingCollection. input)
        (.getSecretKey (key-id id)))))


(defn- generate-signature
  [data-source
   hash-algorithm-code
   key-algorithm-code
   private-key]
  (let [generator (PGPSignatureGenerator.
                    key-algorithm-code
                    hash-algorithm-code
                    security-provider)]
    (.init generator PGPSignature/BINARY_DOCUMENT private-key)
    (with-open [data (io/input-stream data-source)]
      (let [buffer (byte-array 256)]
        (loop [n (.read data buffer)]
          (when (> n 0)
            (.update generator buffer 0 n)
            (recur (.read data buffer))))))
    (.generate generator)))


(defn sign
  [data-source secret-key passphrase]
  (let [private-key (.extractPrivateKey
                      secret-key
                      passphrase
                      security-provider)]
    (generate-signature
      data-source
      (hash-algorithms *hash-algorithm*)
      (key-algorithm-code secret-key)
      private-key)))


(defn print-key-info
  "Prints information about the given key."
  [k]
  (println "Key id:" (Long/toHexString (key-id k))
           (if (.isMasterKey k) "(master key)" "(subkey)"))
  (when (instance? PGPSecretKey k)
    (if (.isPrivateKeyEmpty k)
      (println "    Private key is not available.")
      (if (.isSigningKey k)
        (println "    Private key is available for decryption and signing.")
        (println "    Private key is available for decryption."))))
  (let [public-key (if (instance? PGPSecretKey k) (.getPublicKey k) k)
        strength (.getBitStrength public-key)
        algorithm (or (key-algorithm public-key) "UNKNOWN")
        fingerprint (->> (.getFingerprint public-key)
                         (map (partial format "%02X"))
                         string/join)]
    (if (.isEncryptionKey public-key)
      (println "    Public key is available for encryption.")
      (println "    Public key is available."))
    (println "    Algorithm:" algorithm (str \( strength " bits)"))
    (println "    Fingerprint:"
             (str (subs fingerprint 0 (- (count fingerprint) 8)) \/
                  (subs fingerprint (- (count fingerprint) 8))))
    (doseq [uid (.getUserIDs public-key)]
      (println "    User ID:" uid))))
