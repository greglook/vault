(ns mvxcvi.vault.blob.store)


;; PROTOCOL

(defprotocol BlobStore
  (enumerate
    [this]
    "Enumerates the stored blobs, returning a sequence of BlobRefs.")

  (content-stream
    [this blobref]
    "Returns a stream of the byte content for the referenced blob, if it is
    stored.")

  (store-content!
    [this content]
    "Stores the given byte stream and returns the blob reference.")

  (blob-info
    [this blobref]
    "Returns a map of metadata about the blob, if it is stored. Properties are
    implementation-specific, but should include :size and potentially
    :location."))


(defn contains-blob?
  "Determines whether the store contains the referenced blob."
  [store blobref]
  (not (nil? (blob-info store blobref))))
