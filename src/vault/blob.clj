(ns vault.blob
  (:refer-clojure :exclude [list ref])
  (:require [clojure.string :as string]
            digest))


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
                  ", must be one of: " (string/join ", " digest-algorithms))))))



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


; FIXME: this doesn't need to be here, could be declared later to remove dependency on vault.data
;(data/extend-tagged-str BlobRef vault/ref)


(defn parse-identifier
  "Parses a hash identifier string into a blobref. Accepts either a hash URN
  or the shorter \"algo:digest\" format."
  [id]
  (let [id (if (re-find #"^urn:" id) (subs id 4) id)
        id (if (re-find #"^hash:" id) (subs id 5) id)
        [algorithm digest] (string/split id #":" 2)
        algorithm (keyword algorithm)]
    (assert-valid-digest algorithm)
    (BlobRef. algorithm digest)))


(defn ref
  "Constructs a BlobRef out of the arguments."
  ([x]
   (if (instance? BlobRef x) x
     (parse-identifier (str x))))
  ([algorithm digest]
   (let [algorithm (keyword algorithm)]
     (assert-valid-digest algorithm)
     (BlobRef. algorithm digest))))



;; BLOB STORE PROTOCOL

(defprotocol BlobStore
  (algorithm
    [this]
    "Returns the algorithm in use by the blob store.")

  (enumerate
    [this opts]
    "Enumerates the stored blobs, returning a sequence of BlobRefs.
    Options should be keyword/value pairs from the following:
    * :start - start enumerating blobrefs lexically following this string
    * :prefix - only return blobrefs matching the given string
    * :count - limit the number of results returned")

  (stat
    [this blobref]
    "Returns a map of metadata about the blob, if it is stored. Properties are
    implementation-specific, but should include:
    * :size - blob size in bytes
    * :since - date blob was added to store
    Optionally, other attributes may also be included:
    * :content-type - a guess at the type of content stored in the blob
    * :location - a resource location for the blob")

  (open
    ^java.io.InputStream
    [this blobref]
    "Opens a stream of byte content for the referenced blob, if it is stored.")

  (store!
    [this content]
    "Stores the given byte stream and returns the blob reference.")

  (remove!
    [this blobref]
    "Remove the referenced blob from this store. Returns true if the store
    contained the blob when this method was called."))


(defn list
  "Enumerates the stored blobs, returning a sequence of BlobRefs.
  Options should be keyword/value pairs from the following:
  * :start - start enumerating blobrefs lexically following this string
  * :prefix - only return blobrefs matching the given string
  * :count - limit the number of results returned"
  ([store]
   (enumerate store nil))
  ([store opts]
   (enumerate store opts))
  ([store opt-key opt-val & opts]
   (->> opts
        (partition 2)
        (cons [opt-key opt-val])
        (into {})
        (enumerate store))))


(defn contains-blob?
  "Determines whether the store contains the referenced blob."
  [store blobref]
  (not (nil? (stat store blobref))))


(defn select-refs
  "Selects blobrefs from a lazy sequence based on input criteria."
  [opts blobrefs]
  (let [{:keys [start prefix]} opts
        blobrefs (if-let [start (or start prefix)]
                   (drop-while #(< 0 (compare start (str %))) blobrefs)
                   blobrefs)
        blobrefs (if prefix
                   (take-while #(.startsWith (str %) prefix) blobrefs)
                   blobrefs)
        blobrefs (if-let [n (:count opts)]
                   (take n blobrefs)
                   blobrefs)]
    blobrefs))



;; CONTENT HASHING

(defn digest
  "Calculates the blob reference for the given content."
  [algorithm content]
  (assert-valid-digest algorithm)
  (let [hashfn (digest-functions algorithm)
        digest ^String (hashfn content)]
    (BlobRef. algorithm (.toLowerCase digest))))
