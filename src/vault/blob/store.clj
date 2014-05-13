(ns vault.blob.store
  (:refer-clojure :exclude [get list load])
  (:require
    byte-streams
    [vault.blob.digest :as digest]))


;; BLOB RECORD

(defrecord Blob [id ^bytes content])


(defmethod print-method Blob
  [v ^java.io.Writer w]
  (let [v (dissoc v :content)]
    (.write w (prn-str v))))


(defn record
  "Constructs a new blob record with the given id and optional content."
  ([id]
   (record id nil))
  ([id content]
   (Blob. id content)))


(defn load
  "Buffers data in memory and hashes it to identify the blob."
  [source]
  (let [content (byte-streams/to-byte-array source)]
    (when-not (empty? content)
      (record (digest/hash content) content))))



;; STORAGE INTERFACE

(defprotocol BlobStore
  "Protocol for content storage providers, keyed by hash ids."

  (enumerate [this opts]
    "Enumerates the ids of the stored blobs with some filtering options. The
    'list' function provides a nicer wrapper around this protocol method.")

  (stat [this id]
    "Returns a blob record with metadata but no content. Properties are
    generally implementation-specific, but may include:
    * :stat/size        blob size in bytes
    * :stat/stored-at   date blob was added to store
    * :stat/origin      a resource location for the blob")

  (get* [this id]
    "Loads content from the store and returns a Blob record. Returns nil if no
    matching content is found. The Blob record may include data as from the
    `stat` function.")

  (put! [this blob]
    "Saves a blob into the store. Returns the blob record, potentially updated
    with `stat` metadata."))


(defprotocol DestructiveBlobStore
  "Additional blob storage protocol to represent direct storage types which can
  excise blob data."

  (delete! [this id]
    "Remove a blob from the store.")

  (destroy!! [this]
    "Completely remove all data from the store."))


(defn list
  "Enumerates the stored blobs, returning a sequence of HashIDs.
  Options should be keyword/value pairs from the following:
  * :after    start enumerating ids lexically following this string
  * :prefix   only return ids matching the given string
  * :limit    limit the number of results returned"
  ([store]
   (enumerate store nil))
  ([store opts]
   (enumerate store opts))
  ([store opt-key opt-val & opts]
   (enumerate store (apply hash-map opt-key opt-val opts))))


(defn get
  "Retrieves data for the given blob and returns the blob record. This function
  verifies that the id matches the actual digest of the data returned."
  [store id]
  (when-let [blob (get* store id)]
    (let [digest (digest/hash (:algorithm id) (:content blob))]
      (when (not= id digest)
        (throw (RuntimeException.
                 (str "Store " store " returned invalid data: requested "
                      id " but got " digest)))))
    blob))


(defn store!
  "Stores data from the given byte source and returns the blob record."
  [store source]
  (when-let [blob (load source)]
    (put! store blob)))



;; UTILITY FUNCTIONS

(defn select-ids
  "Selects hash identifiers from a lazy sequence based on input criteria.
  Available options:
  * :after    start enumerating ids lexically following this string
  * :prefix   only return ids matching the given string
  * :limit    limit the number of results returned"
  [opts ids]
  (let [{:keys [after prefix limit]} opts
        after (or after prefix)]
    (cond->> ids
      after  (drop-while #(pos? (compare after (str %))))
      prefix (take-while #(.startsWith (str %) prefix))
      limit  (take limit))))
