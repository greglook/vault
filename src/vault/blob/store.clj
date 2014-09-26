(ns vault.blob.store
  (:refer-clojure :exclude [get list read])
  (:require
    byte-streams
    [vault.blob.digest :as digest]))


;;;;; BLOB RECORD ;;;;;

(defrecord Blob [id ^bytes content])


(defn empty-blob
  "Constructs a new blob record with the given hash-id and no content. This is
  mostly useful for answering `stat` calls."
  [id]
  (Blob. id nil))


(defn read
  "Reads data into memory from the given source and hashes it to identify the
  blob. This can handle any source supported by the byte-streams library."
  [source]
  (let [content (byte-streams/to-byte-array source)]
    (when-not (empty? content)
      (Blob. (digest/hash content) content))))


(defn write
  "Writes blob data to a byte stream."
  [blob sink]
  (when-let [content (:content blob)]
    (byte-streams/transfer content sink)))



;;;;; STORAGE INTERFACE ;;;;;

(defprotocol BlobStore
  "Protocol for content storage providers, keyed by hash ids."

  (enumerate [store opts]
    "Enumerates the ids of the stored blobs with some filtering options. The
    'list' function provides a nicer wrapper around this protocol method.")

  (stat [store id]
    "Returns a blob record with metadata but no content. Properties are
    generally implementation-specific, but may include:
    * :stat/size        blob size in bytes
    * :stat/stored-at   date blob was added to store
    * :stat/origin      a resource location for the blob")

  (get [store id]
    "Loads content from the store and returns a Blob record. Returns nil if no
    matching content is found. The Blob record may include data as from the
    `stat` function.")

  (put! [store blob]
    "Saves a blob into the store. Returns the blob record, potentially updated
    with `stat` metadata.")

  (delete! [store id]
    "Removes a blob from the store."))


(defn list
  "Enumerates the stored blobs, returning a sequence of HashIDs.
  See `select-ids` for the available query options."
  ([store]
   (enumerate store nil))
  ([store opts]
   (enumerate store opts))
  ([store opt-key opt-val & opts]
   (enumerate store (apply hash-map opt-key opt-val opts))))


(defn get'
  "Retrieves data for the given blob and returns the blob record. This function
  verifies that the id matches the actual digest of the data returned."
  [store id]
  (when-let [blob (get store id)]
    (let [digest (digest/hash (:algorithm id) (:content blob))]
      (when (not= id digest)
        (throw (RuntimeException.
                 (str "Store " store " returned invalid data: requested "
                      id " but got " digest)))))
    blob))


(defn store!
  "Stores data from the given byte source and returns the blob record. This
  method accepts any data source which can be handled as a byte stream by
  `read`."
  [store source]
  (when-let [blob (read source)]
    (put! store blob)))


(defn scan-size
  "Scans the blobs in a store to determine the total stored content size."
  [store]
  (reduce + 0 (map (comp :stat/size (partial stat store)) (list store))))



;;;;; UTILITY FUNCTIONS ;;;;;

(defn select-ids
  "Selects hash identifiers from a sequence based on input criteria.
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
