(ns mvxcvi.vault.blob.store)


;; PROTOCOL

(defprotocol BlobStore
  (stat [this blobref] "Returns the size of the referenced blob in bytes, if it is stored.")
  (get-blob [this blobref] "Retrieves the bytes for a given blob.")
  (put-blob [this data] "Stores the given bytes and returns the blobref.")
  (enumerate [this] "Enumerates the stored blobs."))

; TODO: should `enumerate` include the stat results?
