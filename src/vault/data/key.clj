(ns vault.data.key
  "Functions to read blobs containing PGP public-key data."
  (:require
    [mvxcvi.crypto.pgp :as pgp]
    [vault.blob.content :as content]
    [vault.data.struct :as struct])
  (:import
    (org.bouncycastle.openpgp
      PGPPublicKey
      PGPPublicKeyRing)))


(def ^:const ^:private key-header
  "Header string which must appear as the first characters in a pgp key blob."
  "-----BEGIN PGP PUBLIC KEY")


(defn- check-header
  "Reads the first few bytes from a blob's content to determine whether it is a
  PGP public key blob. The result is true if the header matches, otherwise
  false."
  [^bytes content]
  (let [header (.getBytes key-header)
        len (count header)]
    (= (seq header) (take len (seq content)))))


(defn- extract-key
  "Extracts a public key from the given PGP value. If the value is a keyring,
  the first key will be returned."
  [value]
  (condp = (class value)
    PGPPublicKey
    value

    PGPPublicKeyRing
    (first (pgp/list-public-keys value))

    (throw (IllegalArgumentException.
             (str "Class " (class value) " does not represent a public key.")))))


(defn parse-key
  "Reads the contents of a blob and attempts to parse it as a PGP key. Returns
  an updated blob record, or nil if the content is not a PGP public key."
  [blob]
  (when (check-header (:content blob))
    (let [public-key (->> blob :content pgp/decode first extract-key)]
      (struct/data-attrs blob
        :pgp/public-key
        [public-key]))))


(defn key->blob
  "Constructs a key blob from a PGP public key."
  [value]
  (when-let [public-key (extract-key value)]
    (struct/data-attrs
      (content/read (pgp/encode-ascii public-key))
      :pgp/public-key
      [public-key])))
