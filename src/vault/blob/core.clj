(ns vault.blob.core
  (:refer-clojure :exclude [get hash list load])
  (:require
    byte-streams
    [clojure.string :as str])
  (:import
    java.security.MessageDigest))


;; UTILITY FUNCTIONS

(defn- zero-pad
  "Pads a string with leading zeroes up to the given width."
  [width value]
  (let [string (str value)]
    (if (< width (count string))
      string
      (-> width
          (- (count string))
          (repeat "0")
          str/join
          (str string)))))


(defn- hex-str
  [^bytes value]
  (let [width (* 2 (count value))
        hex (-> (BigInteger. 1 value)
                (.toString 16)
                str/lower-case)]
    (zero-pad width hex)))



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

(def hash-algorithms
  "Map of content hashing algorithms to system names."
  {:md5    "MD5"
   :sha1   "SHA-1"
   :sha256 "SHA-256"})


(def ^:dynamic *hash-algorithm*
  "Default digest algorithm to use for content hashing."
  :sha256)


(defmacro with-algorithm
  "Executes a body of expressions with the given default digest algorithm."
  [algorithm & body]
  `(binding [*hash-algorithm* ~algorithm]
     ~@body))


(defn hash
  "Calculates the hash digest of the given byte array. Returns a HashID."
  [algo ^bytes content]
  {:pre [(contains? hash-algorithms algo)]}
  (->HashID
    algo
    (->
      (hash-algorithms algo)
      MessageDigest/getInstance
      (.digest content)
      hex-str)))



;; BLOB RECORD

(defrecord Blob [id content])


(defmethod print-method Blob
  [v ^java.io.Writer w]
  (let [v (dissoc v :content)]
    (.write w (prn-str v))))


(defn record
  "Constructs a new blob record with the given id and optional content."
  ([id]
   (record id nil))
  ([id content]
   (->Blob id content)))


(defn load
  "Buffers data in memory and hashes it to identify the blob."
  [source]
  (let [content (byte-streams/to-byte-array source)]
    (when-not (empty? content)
      (let [id (hash *hash-algorithm* content)]
        (record id content)))))



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
