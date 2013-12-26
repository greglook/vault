(ns vault.blob.core
  (:refer-clojure :exclude [contains? list])
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [vault.blob.digest :as digest])
  (:import
    java.io.ByteArrayOutputStream))


;; BLOB STORAGE

(defprotocol BlobStore
  "Protocol for content storage providers, keyed by blobrefs."

  (-list [this opts]
    "Enumerates the stored blobs with some filtering options.")

  (-stat [this id]
    "Returns the stored status metadata for a blob.")

  (-open [this id]
    "Returns a vector of the blob status and an open input stream of the
    stored data.")

  (-store! [this data]
    "Stores blob data content, optionally with associated status metadata.")

  (-remove! [this id]
    "Returns true if the store contained the blob."))



;; BLOB DATA

(defrecord BlobData
  [id status content])


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
