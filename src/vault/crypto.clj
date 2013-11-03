(ns vault.crypto
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import
    (java.security Security)
    (org.bouncycastle.jce.provider BouncyCastleProvider)
    (org.bouncycastle.openpgp PGPUtil
                              PGPPublicKey
                              PGPPublicKeyRing
                              PGPPublicKeyRingCollection
                              PGPSecretKeyRingCollection)))


(Security/addProvider (new BouncyCastleProvider))


(def ^:private public-key-algorithms
  "Map of numeric codes to public-key algorithm names."
  (->> (.getFields org.bouncycastle.bcpg.PublicKeyAlgorithmTags)
       (map #(vector (.getInt % nil) (.getName %)))
       (into {})))



;; KEY FUNCTIONS

(defn key-algorithm
  "Returns the standard algorithm name for the key."
  [k]
  (.getAlgorithm k))


(defn key-format
  "Returns the name of the primary encoding format of the key."
  [k]
  (.getFormat k))


(defn encode-key
  "Returns the key in its primary encoding format."
  [k]
  (.getEncoded k))


(defn private-key
  "Converts the argument to a private key."
  [k]
  (cond (instance? PrivateKey k) k
        (instance? KeyPair k) (.getPrivate k)
        :else (throw (IllegalArgumentException.
                       (str "Unknown private-key type: " k)))))


(defn public-key
  "Converts the argument to a public key."
  [k]
  (cond (instance? PublicKey k) k
        (instance? KeyPair k) (.getPublic k)
        :else (throw (IllegalArgumentException.
                       (str "Unknown public-key type: " k)))))



;; TEST FUNCTIONALITY

(defn load-public-keys
  "Loads the public keys present in the keyring file named by the path."
  [path]
  (with-open [input (PGPUtil/getDecoderStream
                      (io/input-stream path))]
    (->> input
         (new PGPPublicKeyRingCollection)
         .getKeyRings
         iterator-seq
         (mapcat #(iterator-seq (.getPublicKeys %))))))


(defn load-secret-keys
  "Loads the private keyrings present in the keyring file named by the path."
  [path]
  (with-open [input (PGPUtil/getDecoderStream
                      (io/input-stream path))]
    (->> input
         (new PGPSecretKeyRingCollection)
         .getKeyRings
         iterator-seq
         (mapcat #(iterator-seq (.getSecretKeys %))))))


(defn print-key-info
  "Prints information about the given key."
  [k]
  (println "Key id:" (Long/toHexString (.getKeyID k)))
  (println "        Algorithm:" (public-key-algorithms (.getAlgorithm k) "UNKNOWN"))
  (println "        Fingerprint:" (->> (.getFingerprint k)
                                       (map (partial format "%02X"))
                                       string/join)))
