(ns vault.blob.filter.compress
  (:require
    [vault.blob.filter :refer [BlobCodec]])
  (:import
    (java.util.zip
      GZIPInputStream
      GZIPOutputStream)))


(defrecord GZIPBlobCodec
  []

  BlobCodec

  (wrap-output [this stream]
    [nil (GZIPOutputStream. stream)])

  (wrap-input [this status stream]
    (GZIPInputStream. stream)))


(defn gzip-codec [] (->GZIPBlobCodec))
