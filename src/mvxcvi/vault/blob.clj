(ns mvxcvi.vault.blob
  (:require (clojure [pprint :refer [pprint]]
                     [string :as string])))



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



(defn ref->path
  "Builds a filesystem path out of the given blobref"
  [blobref]
  (let [[algo digest] (string/split blobref #":" 2)]
    (string/join \/ [algo (subs digest 0 3) (subs digest 3)])))

