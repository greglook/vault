(ns vault.blob.store)


(defprotocol BlobStore
  "Protocol for content storage providers, keyed by blobrefs."

  (-list [this opts]
    "Enumerates the stored blobs with some filtering options.")

  (-stat [this blobref]
    "Returns the stored status metadata for a blob.")

  (-open [this blobref]
    "Returns a vector of the blob status and an open input stream of the
    stored data.")

  (-store! [this blobref status stream]
    "Stores the contents of the input stream for the blob, optionally with
    associated status metadata.")

  (-remove! [this blobref]
    "Returns true if the store contained the blob."))



;; UTILITY FUNCTIONS

(defn select-refs
  "Selects blobrefs from a lazy sequence based on input criteria."
  [opts blobrefs]
  (let [{:keys [start prefix]} opts
        blobrefs (if-let [start (or start prefix)]
                   (drop-while #(pos? (compare start (str %))) blobrefs)
                   blobrefs)
        blobrefs (if prefix
                   (take-while #(.startsWith (str %) prefix) blobrefs)
                   blobrefs)
        blobrefs (if-let [n (:count opts)]
                   (take n blobrefs)
                   blobrefs)]
    blobrefs))
