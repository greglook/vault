(ns vault.blob.core
  (:refer-clojure :exclude [contains? list ref])
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    (vault.blob
      [digest :as digest]))
  (:import
    java.io.ByteArrayOutputStream))


(defrecord BlobData
  [ref status content])



(defn ref
  "Constructs a BlobRef out of the arguments."
  ([x]
   (cond
     (instance? digest/HashIdentifier x) x
     (instance? BlobData x) (:ref x)
     :else (digest/parse-id (str x))))
  ([algorithm digest]
   (let [algorithm (keyword algorithm)]
     (digest/->HashID algorithm digest))))




;; BLOB DATA

(defmulti load-bytes
  "Loads data from the given source into a byte array."
  type)


(defmethod load-bytes String
  [^String source]
  (.getBytes source "UTF-8"))


(defmethod load-bytes java.io.InputStream
  [^java.io.InputStream source]
  (with-open [buffer (ByteArrayOutputStream.)]
    (io/copy source buffer)
    (.toByteArray buffer)))


(defmethod load-bytes java.io.File
  [^java.io.File source]
  (with-open [stream (java.io.FileInputStream. source)]
    (load-bytes stream)))


(defn load-blob
  "Buffers a blob in memory and calculates the hash digest. Returns a map with
  :ref and :content keys."
  [source]
  (let [content (load-bytes source)
        blobref (apply ->BlobRef (digest/hash content))]
    (->BlobData blobref {} content)))



;; BLOB STORAGE

(defprotocol BlobStore
  "Protocol for content storage providers, keyed by blobrefs."

  (-list [this opts]
    "Enumerates the stored blobs with some filtering options.")

  (-stat [this blobref]
    "Returns the stored status metadata for a blob.")

  (-open [this blobref]
    "Returns a vector of the blob status and an open input stream of the
    stored data.")

  (-store! [this data]
    "Stores blob data content, optionally with associated status metadata.")

  (-remove! [this blobref]
    "Returns true if the store contained the blob."))


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
   ; Create a blobref with the digest value, then pass to the blob store.
  ([store encoders input-stream]
   (let [blobref nil]
     (blobs/-store! store blobref nil input-stream))))


(defn remove!
  "Remove the referenced blob from this store. Returns true if the store
  contained the blob when this method was called."
  [store blobref]
  (blobs/-remove! store blobref))
