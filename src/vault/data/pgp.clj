(ns vault.data.pgp
  "Functions to read blobs containing pgp data."
  (:require
    [vault.blob.core :as blob]
    [mvxcvi.crypto.pgp :as pgp])
  (:import
    mvxcvi.crypto.pgp.keyring.KeyRing
    (org.bouncycastle.openpgp
      PGPPrivateKey
      PGPPublicKey
      PGPPublicKeyRing
      PGPSignature)))


;;;;; CONFIGURATION ;;;;;

(def ^:const blob-header
  "Magic header which must appear as the first characters in a pgp blob."
  "-----BEGIN PGP ")


(defn- pgp-value
  "Converts some complex PGP types into a subset of simpler values."
  [value]
  (condp = (class value)
    PGPPublicKey
    value

    PGPPublicKeyRing
    (let [pubkeys (pgp/list-public-keys value)]
      (if (= 1 (count pubkeys))
        (first pubkeys)
        (vec pubkeys)))))


(def ^:private pgp-types
  "Map of PGP classes to type keywords in the 'pgp' namespace."
  {PGPPrivateKey :pgp/private-key
   PGPPublicKey  :pgp/public-key
   PGPSignature  :pgp/signature})


(defn pgp-type
  "Determines the 'type' of the given value. This maps PGP object classes to
  type keywords in the pgp namespace."
  [value]
  (let [c (class value)]
    (pgp-types c c)))



;;;;; SERIALIZATION ;;;;;

(defn pgp-blob
  "Constructs a blob from a PGP object."
  [value]
  (assoc (blob/load (pgp/encode-ascii value))
    :data/values [(pgp-value value)]
    :data/type (pgp-type value)))



;;;;; DESERIALIZATION ;;;;;

(defn- check-header
  "Reads the first few bytes from a blob's content to determine whether it is a
  PGP blob. The result is true if the header matches, otherwise false."
  [^bytes content]
  (let [header (.getBytes blob-header)
        len (count header)]
    (= (seq header) (take len (seq content)))))


(defn read-blob
  "Reads the contents of a blob and attempts to parse it as a PGP object.
  Returns an updated blob record, or nil if the content is not PGP data."
  [blob]
  (when (check-header (:content blob))
    (let [values (->> blob :content pgp/decode (map pgp-value) vec)]
      (assoc blob
        :data/values values
        :data/type (pgp-type (first values))))))
