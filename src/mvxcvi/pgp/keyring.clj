(ns mvxcvi.pgp.keyring
  "Keyring provider protocols."
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [clojure.string :as string]
    (mvxcvi.pgp
      [core :as pgp :refer [KeyStore]]
      [util :refer [hex-str]]))
  (:import
    (org.bouncycastle.openpgp
      PGPPublicKeyRing
      PGPPublicKeyRingCollection
      PGPSecretKeyRing
      PGPSecretKeyRingCollection
      PGPUtil)))


;; KEYRING UTILITIES

(defn- load-public-keyrings
  "Loads a public keyring file into a sequence of vectors of public keys."
  [source]
  (with-open [stream (PGPUtil/getDecoderStream
                       (byte-streams/to-input-stream source))]
    (map (fn [^PGPPublicKeyRing keyring]
           (vec (iterator-seq (.getPublicKeys keyring))))
         (-> stream
             PGPPublicKeyRingCollection.
             .getKeyRings
             iterator-seq))))


(defn- load-secret-keyrings
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


(defn- find-key
  "Locates a key in a sequence by id. Nested sequences are flattened, so this
  works directly on keyrings and keyring collections."
  [id key-seq]
  (let [id (pgp/key-id id)]
    (some #(when (= id (pgp/key-id %)) %)
          (flatten key-seq))))



;; KEYRING PROVIDER

(defrecord PGPKeyring [pubring secring])

(extend-protocol KeyStore
  PGPKeyring

  (list-public-keys [this]
    (-> this :pubring load-public-keyrings flatten))

  (get-public-key [this id]
    (->> this :pubring load-public-keyrings (find-key id)))

  (list-secret-keys [this]
    (-> this :secring load-secret-keyrings flatten))

  (get-secret-key [this id]
    (->> this :secring load-secret-keyrings (find-key id))))


(defn pgp-keyring
  "Constructs a PGPKeyring for the given keyring files."
  [pubring secring]
  (->PGPKeyring (io/file pubring) (io/file secring)))



;; CACHING PROVIDER

#_
(defrecord PrivateKeyCache
  [provider store]

  KeyStore

  (get-public-key
    [this id]
    (pgp/get-public-key (:provider this) id))

  (get-secret-key
    [this id]
    (pgp/get-secret-key (:provider this) id))

  (get-private-key
    [this id]
    (let [id (pgp/key-id id)]
      (or (get @(:store this) id)
          (when-let [privkey (pgp/get-private-key (:provider this) id)]
            (swap! (:store this) assoc id privkey)
            privkey))))

  (get-private-key
    [this id passphrase]
    (let [id (pgp/key-id id)]
      (or (get @(:store this) id)
          (when-let [privkey (pgp/get-private-key (:provider this) id passphrase)]
            (swap! (:store this) assoc id privkey)
            privkey)))))


#_
(defn key-cache
  "Wraps a key provider in a layer that keeps unlocked private keys in a map."
  [provider]
  (->PrivateKeyCache provider (atom {})))



;; INTERACTIVE PROVIDER

#_
(defn interactive-unlocker
  "Wraps a key provider in a layer that will request a passphrase on the
  command-line when a private key needs to be unlocked."
  [provider]
  (reify KeyStore

    (get-public-key
      [_ id]
      (pgp/get-public-key provider id))

    (get-secret-key
      [_ id]
      (pgp/get-secret-key provider id))

    (get-private-key
      [_ id]
      (let [id (pgp/key-id id)]
        (println "Passphrase for private key " (hex-str id) ":")
        (pgp/get-private-key provider id (read-line))))

    (get-private-key
      [_ id passphrase]
      (pgp/get-private-key provider id passphrase))))
