(ns vault.blob.core
  (:refer-clojure :exclude [get hash list load])
  (:require
    byte-streams
    [clojure.string :as str]
    [mvxcvi.crypto.digest :as digest]
    [puget.data :as edn]))


;; HASH IDENTIFIERS

(defrecord HashID
  [algorithm digest]

  Comparable

  (compareTo [this that]
    (if (= this that)
      0
      (->> [this that]
           (map (juxt :algorithm :digest))
           (apply compare))))

  Object

  (toString [this]
    (str (name algorithm) \: digest)))


(defn path-str
  "Converts a hash id into a path-safe string. This differs from the normal
  representation in that the colon (:) is replaced with a hyphen (-). This lets
  the identifier be used in file paths and URLs."
  [id]
  (str (name (:algorithm id)) \- (:digest id)))


(defn parse-id
  "Parses a string into a hash identifier. This function accepts the following
  formats:
  - urn:hash:{algo}:{digest}
  - hash:{algo}:{digest}
  - {algo}:{digest}
  - {algo}-{digest}"
  [^String id]
  (let [id (if (.startsWith id "urn:") (subs id 4) id)
        id (if (.startsWith id "hash:") (subs id 5) id)
        [algorithm digest] (str/split id #"[:-]" 2)
        algorithm (keyword algorithm)]
    (->HashID algorithm digest)))


(edn/extend-tagged-str HashID vault/ref)
(edn/register-reader! 'vault/ref parse-id)


(defn hash-id
  "Constructs a hash identifier from the arguments."
  ([x]
   (cond
     (instance? HashID x) x
     :else (parse-id (str x))))
  ([algorithm digest]
   (let [algo (keyword algorithm)]
     (->HashID algo digest))))


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



;; CONTENT HASHING

(def ^:dynamic *digest-algorithm*
  "Default digest algorithm to use for content hashing."
  :sha256)


(defmacro with-algorithm
  "Executes a body of expressions with the given default digest algorithm."
  [algorithm & body]
  `(binding [*digest-algorithm* ~algorithm]
     ~@body))


(defn hash
  "Calculates the hash digest of the given data source. Returns a HashID."
  [algo content]
  (->HashID algo (digest/hash-content algo content)))



;; BLOB RECORD

(defrecord Blob [id content])


(defmethod print-method Blob
  [v ^java.io.Writer w]
  (->> w
       (dissoc :content)
       prn-str
       (.write w)))


(defn load
  "Buffers data in memory and hashes it to identify the blob."
  [source]
  (let [content (byte-streams/to-byte-array source)]
    (when-not (empty? content)
      (let [id (hash *digest-algorithm* content)]
        (->Blob id content)))))



;; STORAGE INTERFACE

(defprotocol BlobStore
  "Protocol for content storage providers, keyed by hash ids."

  (enumerate [this opts]
    "Enumerates the ids of the stored blobs with some filtering options. The
    'list' function provides a nicer wrapper around this protocol method.")

  (stat [this id]
    "Returns a map of metadata about the blob, if it is stored. Properties are
    generally, implementation-specific, but may include:
    * :meta/size        blob size in bytes
    * :meta/stored-at   date blob was added to store
    * :meta/origin      a resource location for the blob")

  (get* [this id]
    "Loads content from the store and returns a Blob record. Returns nil if no
    matching content is found. The Blob record may include data as from the
    `stat` function.")

  (put! [this blob]
    "Saves a blob into the store. Returns the blob record, potentially updated
    with `stat` metadata."))


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
    (let [content-id (hash (:algorithm id) (:content blob))]
      (when-not (= id content-id)
        (throw (RuntimeException.
                 (str "Store " store " returned invalid data: requested "
                      id " but got " content-id)))))
    blob))


(defn store!
  "Stores data from the given byte source and returns the blob record."
  [store source]
  (when-let [blob (load source)]
    (put! store blob)))
