(ns mvxcvi.vault.blob
  (:require (clojure [pprint :refer [pprint]]
                     [string :as string]))
  (:import (java.security MessageDigest)))


(def hash-algorithms
  "Map of the available content hashing algorithms."
  {:sha1   "SHA-1"
   :sha256 "SHA-256"})


(defprotocol BlobStore
  (stat [this blobref] "Returns the size of the referenced blob in bytes, if it is stored.")
  (get-blob [this blobref] "Retrieves the bytes for a given blob.")
  (put-blob [this data] "Stores the given bytes and returns the blobref.")
  (enumerate [this] "Enumerates the stored blobs.")) ; TODO: should `enumerate` include the blob sizes?


; TODO: this should probably be a multimethod; non-clojure data structures
; (like a sequence of raw bytes) should have different serialization semantics.
(defn serialize-blob
  "Produces a string representing the 'canonical' serialization of the
   given value."
  [value]
  ; TODO: customize the pretty printer style
  (string/trim (with-out-str (pprint value))))


(defn hash-blob
  "Computes the hash of the given blob."
  [blob]
  (let [algo   (MessageDigest/getInstance "SHA-256") ; TODO: don't hardcode
        data   (.getBytes blob)
        digest (.digest algo data)]
    (->> digest (map (partial format "%02x")) string/join)))


(defn blob->ref
  "Calculates a reference hash for the given blob data."
  [blob]
  (str "sha256:" (hash-blob blob))) ; TODO: don't hardcode


(defn ref->path
  "Builds a filesystem path out of the given blobref"
  [blobref]
  (let [[algo digest] (string/split blobref #":" 2)]
    (string/join \/ [algo (subs digest 0 3) (subs digest 3)])))

