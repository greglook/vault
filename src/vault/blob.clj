(ns vault.blob
  (:require [clojure.string :as string]
            [vault.data :as data]
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


(defn- assert-valid-digest
  [algorithm]
  (when-not (digest-functions algorithm)
    (throw (IllegalArgumentException.
             (str "Unsupported digest algorithm: " algorithm
                  ", must be one of " (string/join ", " digest-algorithms))))))



;; BLOB REFERENCE

(defrecord BlobRef
  [algorithm digest]

  Comparable

  (compareTo [this that]
    (if (= this that)
      0
      (->> [this that]
           (map (juxt :algorithm :digest))
           (apply compare))))

  Object

  (toString [this]
    (str (name algorithm) ":" digest)))


(extend-type BlobRef
  data/TaggedValue
  (tag [this] 'vault/ref)
  (value [this] (str this)))


(defmethod print-method BlobRef
  [value ^java.io.Writer w]
  (.write w (str "#vault/ref " \" value \")))



;; CONSTRUCTION

(defn parse-address
  "Parses an address string into a blobref. Accepts either a hash URN or the
  shorter \"algo:address\" format."
  [address]
  (let [address (if (re-find #"^urn:" address) (subs address 4) address)
        address (if (re-find #"^hash:" address) (subs address 5) address)
        [algorithm digest] (string/split address #":" 2)
        algorithm (keyword algorithm)]
    (assert-valid-digest algorithm)
    (BlobRef. algorithm digest)))


(defn make-blobref
  "Constructs a blobref out of the arguments."
  ([x]
   (if (instance? BlobRef x)
     x
     (parse-address (str x))))
  ([algorithm digest]
   (let [algorithm (keyword algorithm)]
     (assert-valid-digest algorithm)
     (BlobRef. algorithm digest))))


(defn hash-content
  "Calculates the blob reference for the given content."
  [algorithm content]
  (assert-valid-digest algorithm)
  (let [hashfn (digest-functions algorithm)
        digest ^String (hashfn content)]
    (BlobRef. algorithm (.toLowerCase digest))))
