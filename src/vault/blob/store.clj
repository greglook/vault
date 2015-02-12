(ns vault.blob.store
  "Blob storage protocol and functions.

  When blob records are returned from a blob store, they may include 'stat'
  metadata about the blobs:

  - `:stat/size`        content size in bytes
  - `:stat/stored-at`   time blob was added to the store
  - `:stat/origin`      resource location for the blob"
  (:refer-clojure :exclude [get list])
  (:require
    [vault.blob.content :as content]))


;; ## Storage Interface

(defprotocol BlobStore
  "Protocol for content storage keyed by hash identifiers."

  (enumerate
    [store opts]
    "Enumerates the ids of the stored blobs with some filtering options. The
    'list' function provides a nicer wrapper around this protocol method.")

  (stat
    [store id]
    "Returns a blob record with metadata but no content.")

  (get*
    [store id]
    "Loads content for a hash-id and returns a blob record. Returns nil if no
    blob is stored. The blob should include stat metadata.")

  (put!
    [store blob]
    "Saves a blob into the store. Returns the blob record, updated with stat
    metadata.")

  (delete!
    [store id]
    "Removes a blob from the store.")

  (erase!!
    [store]
    "Removes all blobs from the store."))


(defn list
  "Enumerates the stored blobs, returning a sequence of `HashID`s.
  See `select-ids` for the available query options."
  ([store]
   (enumerate store nil))
  ([store opts]
   (enumerate store opts))
  ([store opt-key opt-val & opts]
   (enumerate store (apply hash-map opt-key opt-val opts))))


(defn get
  "Loads content for a hash-id and returns a blob record. Returns nil if no
  blob is stored. The blob should include stat metadata.

  The digest of the loaded content is checked against the requested hash-id.
  Throws an exception if the digest does not match."
  [store id]
  (when-let [blob (get* store id)]
    (let [digest (content/hash (:algorithm id) (:content blob))]
      (when (not= id digest)
        (throw (RuntimeException.
                 (str "Store " store " returned invalid content: requested "
                      id " but got " digest)))))
    blob))


(defn store!
  "Stores content from a byte source in a blob store and returns the blob
  record. This method accepts any source which can be handled as a byte
  stream by the byte-streams library."
  [store source]
  (when-let [blob (content/read source)]
    (put! store blob)))



;; ## Utility Functions

(defn select-ids
  "Selects hash identifiers from a sequence based on input criteria.
  Available options:

  - `:after`   start enumerating ids lexically following this string
  - `:prefix`  only return ids starting with the given string
  - `:limit`   limit the number of results returned"
  [opts ids]
  (let [{:keys [after prefix limit]} opts
        after (or after prefix)]
    (cond->> ids
      after  (drop-while #(pos? (compare after (str %))))
      prefix (take-while #(.startsWith (str %) prefix))
      limit  (take limit))))


(defn scan-size
  "Scans the blobs in a store to determine the total stored content size."
  [store]
  (reduce + 0 (map (comp :stat/size (partial stat store)) (list store))))
