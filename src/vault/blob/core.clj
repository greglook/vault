(ns vault.blob.core
  (:refer-clojure :exclude [contains? get hash list])
  (:require
    byte-streams
    [clojure.string :as string]
    [vault.util.digest :as digest])
  (:import
    java.nio.ByteBuffer
    java.security.MessageDigest))


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
    (str (name algorithm) ":" digest)))


(defn parse-id
  "Parses a hash identifier string into a hash identifier. Accepts either a
  hash URN or the shorter \"algo:digest\" format."
  [id]
  (let [id (if (re-find #"^urn:" id) (subs id 4) id)
        id (if (re-find #"^hash:" id) (subs id 5) id)
        [algorithm digest] (string/split id #":" 2)
        algorithm (keyword algorithm)]
    (->HashID algorithm digest)))


(defn hash-id
  "Coerces the argument to a HashID."
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
  * :after  - start enumerating ids lexically following this string
  * :prefix - only return ids matching the given string
  * :limit  - limit the number of results returned"
  [opts ids]
  (let [{:keys [after prefix]} opts
        ids (if-let [after (or after prefix)]
              (drop-while #(pos? (compare after (str %))) ids)
              ids)
        ids (if prefix
              (take-while #(.startsWith (str %) prefix) ids)
              ids)
        ids (if-let [n (:limit opts)]
              (take n ids)
              ids)]
    ids))



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



;; STORAGE INTERFACE

(defprotocol BlobStore
  "Protocol for content storage providers, keyed by hash ids."

  (enumerate [this opts]
    "Enumerates the ids of the stored blobs with some filtering options. The
    'list' function provides a nicer wrapper around this protocol method.")

  (stat [this id]
    "Returns a map of metadata about the blob, if it is stored. Properties are
    implementation-specific, but may include:
    * :size         - blob size in bytes
    * :created-at   - date blob was added to store
    * :location     - a resource location for the blob")

  (open
    ^java.io.InputStream
    [this id]
    "Opens a stream of byte content for the blob, if it is stored. The
    'get' function provides a wrapper around this method which buffers the
    blob content.")

  (store! [this blob]
    "Persists blob content in the store. Clients should prefer the 'put!'
    function, which handles hashing the data.")

  (delete! [this id]
    "Removes the referenced blob from the store. Returns true if the store
    contained the blob.")

  (destroy!! [this]
    "Completely removes all stored blob data."))


(defn blob-data
  "Builds a blob data map."
  [id content]
  {:id id
   :content content})


(defn list
  "Enumerates the stored blobs, returning a sequence of HashIDs.
  Options should be keyword/value pairs from the following:
  * :after  - start enumerating ids lexically following this string
  * :prefix - only return ids matching the given string
  * :limit  - limit the number of results returned"
  ([store]
   (enumerate store nil))
  ([store opts]
   (enumerate store opts))
  ([store opt-key opt-val & opts]
   (enumerate store (apply hash-map opt-key opt-val opts))))


(defn contains?
  "Determines whether the store contains the referenced blob."
  [store id]
  (not (nil? (stat store id))))


(defn get
  "Retrieves data for the given blob and returns blob data with buffered
  content. This function verifies that the id matches the actual digest of the
  data returned."
  [store id]
  (with-open [stream (open store id)]
    (when stream
      (let [content (byte-streams/to-byte-array stream)
            data-id (hash (:algorithm id) content)]
        (when-not (= id data-id)
          (throw (RuntimeException.
                   (str "Store " store " returned invalid data: requested "
                        id " but got " data-id))))
        (blob-data id content)))))


(defn put!
  "Stores data from the given byte source and returns the blob's hash id."
  [store source]
  (let [content (byte-streams/to-byte-array source)
        id (hash *digest-algorithm* content)]
    (store! store (blob-data id content))
    id))
