(ns vault.blob.store)


;; BLOB STORE PROTOCOL

(defprotocol BlobStore
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


(defn- adjust-prefix
  "Adds the store's algorithm to a blobref prefix if none is specified."
  [store prefix]
  (if-not (some (partial = \:) prefix)
    (str (name (:algorithm store)) \: prefix)
    prefix))


(defn enumerate-prefixes
  "Lists stored blobs with references matching the given prefixes."
  ([store]
   (enumerate store))
  ([store prefix]
   (let [algorithm (:algorithm store)
         prefix (adjust-prefix store prefix)]
     (->> (enumerate store {:start prefix})
          (take-while #(.startsWith (str %) prefix)))))
  ([store prefix & more]
   (let [prefixes (cons prefix more)]
     (apply concat (for [prefix prefixes] (enumerate-prefixes store prefix))))))
