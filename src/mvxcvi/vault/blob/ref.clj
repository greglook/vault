(ns mvxcvi.vault.blob.ref
  (:require (clojure [string :refer [split]])
            digest))


;; BLOB REFERENCE

(defrecord BlobRef [algorithm address]
  Object
  (toString [this] (str (name algorithm) ":" address)))


(defn parse
  "Parses an address string into a blobref. Accepts either a hash URI or the
  shorter \"algo:address\" format."
  [string]
  (let [string (str string)
        string (if (re-find #"^urn:" string) (subs string 4) string)
        [algorithm address] (split string #":" 2)]
    (BlobRef. (keyword algorithm) address)))



;; CONTENT HASHING

(def ^:private hash-functions
  "Map of content hashing algorithms to functional implementations."
  {:md5    digest/md5
   :sha1   digest/sha-1
   :sha256 digest/sha-256})


(def hash-algorithms
  "Set of available content hashing algorithms."
  (into #{} (keys hash-functions)))


(defn hash-content
  "Calculates the blob reference for the given content."
  [algorithm content]
  (when-not (hash-algorithms algorithm)
    (throw (IllegalArgumentException.
             (str "Unsupported hash algorithm: " algorithm))))
  (let [hashfn (hash-functions algorithm)
        digest (-> content hashfn .toLowerCase)]
    (BlobRef. algorithm digest)))
