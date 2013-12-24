(ns vault.blob.core
  (:refer-clojure :exclude [contains? list ref])
  (:require
    [clojure.string :as string]
    (vault.blob
      [digest :as digest]
      [store :as blobs])))


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


; FIXME: figure out where to put this
;(data/extend-tagged-str BlobRef vault/ref)



;; BLOBREF FUNCTIONS

(defn- parse-identifier
  "Parses a hash identifier string into a blobref. Accepts either a hash URN
  or the shorter \"algo:digest\" format."
  [id]
  (let [id (if (re-find #"^urn:" id) (subs id 4) id)
        id (if (re-find #"^hash:" id) (subs id 5) id)
        [algorithm digest] (string/split id #":" 2)
        algorithm (keyword algorithm)]
    (BlobRef. algorithm digest)))


(defn ref
  "Constructs a BlobRef out of the arguments."
  ([x]
   (if (instance? BlobRef x) x
     (parse-identifier (str x))))
  ([algorithm digest]
   (let [algorithm (keyword algorithm)]
     (BlobRef. algorithm digest))))



;; STORAGE FUNCTIONS

(defn list
  "Enumerates the stored blobs, returning a sequence of BlobRefs.
  Options should be keyword/value pairs from the following:
  * :start  - start enumerating blobrefs lexically following this string
  * :prefix - only return blobrefs matching the given string
  * :count  - limit the number of results returned"
  ([store]
   (blobs/-list store nil))
  ([store opts]
   (blobs/-list store opts))
  ([store opt-key opt-val & opts]
   (blobs/-list store (apply hash-map opt-key opt-val opts))))


(defn stat
  "Returns a map of metadata about the blob, if it is stored. Properties are
  implementation-specific, but may include:
  * :size         - blob size in bytes
  * :since        - date blob was added to store
  * :location     - a resource location for the blob
  * :content-type - a guess at the type of content stored in the blob"
  [store blobref]
  (blobs/-stat store blobref))


(defn contains?
  "Determines whether the store contains the referenced blob."
  [store blobref]
  (not (nil? (blobs/-stat store blobref))))


(defn open
  "Opens a stream of byte content for the referenced blob, if it is stored."
  (^java.io.InputStream
   [store blobref]
   ; TODO: decoders
   (blobs/-open store blobref)))


(defn store!
  "Stores the given byte stream and returns the blob reference."
  ([store stream]
   (store! store [] stream))
   ; TODO: this should serialize the content through some streams into an in-memory buffer.
   ; InputStream > pump > MessageDigestOutputStream > GZIPOutputStream > EncryptionOutputStream > ByteArrayOutputStream
   ; Create a blobref with the digest value, then pass to the blob store.
  ([store encoders input-stream]
   (let [blobref nil]
     (blobs/-store! store blobref nil input-stream))))


(defn remove!
  "Remove the referenced blob from this store. Returns true if the store
  contained the blob when this method was called."
  [store blobref]
  (blobs/-remove! store blobref))
