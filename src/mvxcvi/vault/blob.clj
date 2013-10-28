(ns mvxcvi.vault.blob
  (:require (clojure [string :as string])
            digest))


;; HASH ALGORITHMS

(def ^:private digest-functions
  "Map of content hashing algorithms to functional implementations."
  {:md5    digest/md5
   :sha1   digest/sha-1
   :sha256 digest/sha-256})


(def digest-algorithms
  "Set of available content hashing algorithms."
  (into #{} (keys digest-functions)))



;; BLOB REFERENCE

(defrecord BlobRef
  [algorithm digest]

  Object
  (toString [this]
    (str (name algorithm) ":" digest)))


(defmethod print-method BlobRef
  [value ^java.io.Writer w]
  (.write w (str "#vault/ref " \" value \")))


(defn hash-content
  "Calculates the blob reference for the given content."
  [algorithm content]
  (let [hashfn (digest-functions algorithm)]
    (when-not hashfn
      (throw (IllegalArgumentException.
               (str "Unsupported digest algorithm: " algorithm
                    ", must be one of " (string/join ", " digest-algorithms)))))
    (BlobRef. algorithm (-> content hashfn .toLowerCase))))


(defn parse-address
  "Parses an address string into a blobref. Accepts either a hash URI or the
  shorter \"algo:address\" format."
  [address]
  (let [address (if (re-find #"^urn:" address) (subs address 4) address)
        address (if (re-find #"^hash:" address) (subs address 5) address)
        [algorithm digest] (string/split address #":" 2)]
    (BlobRef. (keyword algorithm) digest)))


(defn make-blobref
  "Constructs a blobref out of the arguments."
  ([x]
   (if (instance? BlobRef x)
     x
     (parse-address (str x))))
  ([algorithm digest]
   (BlobRef. algorithm digest)))
