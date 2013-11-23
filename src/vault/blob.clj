(ns vault.blob
  (:refer-clojure :exclude [contains? list ref])
  (:require [clojure.string :as string]
            digest))


;; RECORDS & PROTOCOLS

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


(defprotocol BlobStore
  "Protocol for content storage providers, keyed by blobrefs."

  (-list [this opts])

  (-stat [this blobref])

  (-open [this blobref])

  (-store! [this content])

  (-remove! [this blobref]))



;; CONTENT HASHING

(def ^:private digest-functions
  "Map of content hashing algorithms to functional implementations."
  {:md5    digest/md5
   :sha1   digest/sha-1
   :sha256 digest/sha-256})


(def digest-algorithms
  "Set of available content hashing algorithms."
  (set (keys digest-functions)))


(def ^:dynamic *digest-algorithm*
  "Default digest algorithm to use when content hashing."
  :sha256)


(defn assert-valid-digest
  [algorithm]
  (when-not (digest-functions algorithm)
    (throw (IllegalArgumentException.
             (str "Unsupported digest algorithm: " algorithm
                  ", must be one of: " (string/join ", " digest-algorithms))))))


(defmacro with-digest-algorithm
  "Executes a body of expressions with the given default digest algorithm."
  [algorithm & body]
  `(binding [*digest-algorithm* ~algorithm]
     (assert-valid-digest *digest-algorithm*)
     ~@body))


(defn digest
  "Calculates the blob reference for the given content."
  ([content]
   (digest *digest-algorithm* content))
  ([algorithm content]
   (assert-valid-digest algorithm)
   (let [hashfn (digest-functions algorithm)
         digest ^String (hashfn content)]
     (BlobRef. algorithm (.toLowerCase digest)))))



;; BLOBREF FUNCTIONS

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


(defn select-refs
  "Selects blobrefs from a lazy sequence based on input criteria."
  [opts blobrefs]
  (let [{:keys [start prefix]} opts
        blobrefs (if-let [start (or start prefix)]
                   (drop-while #(pos? (compare start (str %))) blobrefs)
                   blobrefs)
        blobrefs (if prefix
                   (take-while #(.startsWith (str %) prefix) blobrefs)
                   blobrefs)
        blobrefs (if-let [n (:count opts)]
                   (take n blobrefs)
                   blobrefs)]
    blobrefs))



;; STORAGE FUNCTIONS

(def ^:dynamic *blob-store*
  "Default blob-store to use with the storage functions.")


(defmacro with-blob-store
  "Executes a body of expressions with the given default blob store."
  [store & body]
  `(binding [*blob-store* ~store]
     ~@body))


(defn list
  "Enumerates the stored blobs, returning a sequence of BlobRefs.
  Options should be keyword/value pairs from the following:
  * :start - start enumerating blobrefs lexically following this string
  * :prefix - only return blobrefs matching the given string
  * :count - limit the number of results returned"
  ([]
   (-list *blob-store* nil))
  ([store-or-opts]
   (if (satisfies? BlobStore store-or-opts)
     (-list store-or-opts nil)
     (-list *blob-store* store-or-opts)))
  ([store opts]
   (-list store opts))
  ([store opt-key opt-val & opts]
   (-list store (apply hash-map opt-key opt-val opts))))


(defn stat
  "Returns a map of metadata about the blob, if it is stored. Properties are
  implementation-specific, but should include:
  * :size - blob size in bytes
  * :since - date blob was added to store
  Optionally, other attributes may also be included:
  * :content-type - a guess at the type of content stored in the blob
  * :location - a resource location for the blob"
  ([blobref]
   (-stat *blob-store* blobref))
  ([store blobref]
   (-stat store blobref)))


(defn contains?
  "Determines whether the store contains the referenced blob."
  ([blobref]
   (contains? *blob-store* blobref))
  ([store blobref]
   (not (nil? (-stat store blobref)))))


(defn open
  "Opens a stream of byte content for the referenced blob, if it is stored."
  (^java.io.InputStream
   [blobref]
   (-open *blob-store* blobref))
  (^java.io.InputStream
   [store blobref]
   (-open store blobref)))


(defn store!
  "Stores the given byte stream and returns the blob reference."
  ([content]
   (-store! *blob-store* content))
  ([store content]
   (-store! store content)))


(defn remove!
  "Remove the referenced blob from this store. Returns true if the store
  contained the blob when this method was called."
  ([blobref]
   (-remove! *blob-store* blobref))
  ([store blobref]
   (-remove! store blobref)))
