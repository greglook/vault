(ns vault.store)


;; BLOB STORE PROTOCOL

(defprotocol BlobStore
  ; TODO: get-algorithm method?

  (enumerate
    [this]
    [this opts]
    "Enumerates the stored blobs, returning a sequence of BlobRefs.
    Options should be keyword/value pairs from the following:
    * :start - start enumerating blobrefs lexically following this string
    * :count - limit results returned")

  (stat
    [this blobref]
    "Returns a map of metadata about the blob, if it is stored. Properties are
    implementation-specific, but should include:
    * :size - blob size in bytes
    * :since - date blob was added to store
    Optionally, other attributes may also be included:
    * :content-type - a guess at the type of content stored in the blob
    * :location - a resource location for the blob")

  (open
    ^java.io.InputStream
    [this blobref]
    "Opens a stream of byte content for the referenced blob, if it is stored.")

  (store!
    [this content]
    "Stores the given byte stream and returns the blob reference."))



;; HELPER FUNCTIONS

(defn contains-blob?
  "Determines whether the store contains the referenced blob."
  [store blobref]
  (not (nil? (stat store blobref))))


(defn select-blobrefs
  "Selects blobrefs from a lazy sequence based on input criteria."
  [opts blobrefs]
  (let [{:keys [start prefix]} opts
        blobrefs (if-let [start (or start prefix)]
                   (drop-while #(< 0 (compare start (str %))) blobrefs)
                   blobrefs)
        blobrefs (if prefix
                   (take-while #(.startsWith (str %) prefix) blobrefs)
                   blobrefs)
        blobrefs (if-let [n (:count opts)]
                   (take n blobrefs)
                   blobrefs)]
    blobrefs))
