(ns vault.blob.core
  (:refer-clojure :exclude [contains? list])
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    (vault.blob
      [digest :as digest]
      [store :as blobs]))
  (:import
    java.io.ByteArrayOutputStream))


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
  * :location     - a resource location for the blob"
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
  ([store encoders stream]
   (let [blob (blobs/load-blob stream)]
     (blobs/-store! store blob))))


(defn remove!
  "Remove the referenced blob from this store. Returns true if the store
  contained the blob when this method was called."
  [store id]
  (blobs/-remove! store id))
