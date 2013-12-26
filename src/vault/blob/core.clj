(ns vault.blob.core
  (:refer-clojure :exclude [contains? list])
  (:require
    byte-streams
    [clojure.java.io :as io]
    [clojure.string :as string]
    (vault.blob
      [digest :as digest]))
  (:import
    java.io.ByteArrayOutputStream))


;; CONFIGURATION

(def ^:dynamic *digest-algorithm*
  "Default digest algorithm to use for content hashing."
  :sha256)


(defmacro with-algorithm
  "Executes a body of expressions with the given default digest algorithm."
  [algorithm & body]
  `(binding [*digest-algorithm* ~algorithm]
     ~@body))



;; BLOB STORAGE

(defrecord BlobData
  [id content])


(defprotocol BlobStore
  "Protocol for content storage providers, keyed by hash ids."

  (-list [this opts]
    "Enumerates the stored blobs with some filtering options.")

  (-stat [this id]
    "Returns the stored status metadata for a blob.")

  (-open [this id]
    "Returns a an open input stream of the stored data.")

  (-store! [this blob]
    "Persists blob content in the store.")

  (-remove! [this id]
    "Returns true if the store contained the blob."))


(defn list
  "Enumerates the stored blobs, returning a sequence of HashIDs.
  Options should be keyword/value pairs from the following:
  * :after  - start enumerating ids lexically following this string
  * :prefix - only return ids matching the given string
  * :limit  - limit the number of results returned"
  ([store]
   (-list store nil))
  ([store opts]
   (-list store opts))
  ([store opt-key opt-val & opts]
   (-list store (apply hash-map opt-key opt-val opts))))


(defn stat
  "Returns a map of metadata about the blob, if it is stored. Properties are
  implementation-specific, but may include:
  * :size         - blob size in bytes
  * :since        - date blob was added to store
  * :location     - a resource location for the blob"
  [store id]
  (-stat store id))


(defn contains?
  "Determines whether the store contains the referenced blob."
  [store id]
  (not (nil? (-stat store id))))


(defn open
  "Opens a stream of byte content for the referenced blob, if it is stored."
  (^java.io.InputStream
   [store id]
   (-open store id)))


(defn store!
  "Stores data from the given byte source and returns the blob's hash id."
  ([store source]
   (let [content (byte-streams/to-byte-array source)
         id (digest/hash *digest-algorithm* content)]
     (-store! store (->BlobData id content))
     id)))


(defn remove!
  "Remove the referenced blob from this store. Returns true if the store
  contained the blob when this method was called."
  [store id]
  (-remove! store id))
