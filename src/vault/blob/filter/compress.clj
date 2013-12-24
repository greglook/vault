(ns vault.blob.filter.compress
  (:require
    [vault.blob.filter :refer :all])
  (:import
    (java.util.zip
      GZIPInputStream
      GZIPOutputStream)))


(defrecord GZIPBlobCodec
  "Compresses blob data with GZIP."

  BlobCodec

  (wrap-output [this stream]
    [nil (GZIPOutputStream. stream)])

  (wrap-input [this status stream]
    (GZIPInputStream. stream)))


(defn gzip-codec [] (->GZIPBlobCodec))
