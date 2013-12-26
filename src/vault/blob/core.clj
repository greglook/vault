(ns vault.blob.core
  (:refer-clojure :exclude [contains? hash list])
  (:require
    byte-streams
    [clojure.string :as string])
  (:import
    java.nio.ByteBuffer
    java.security.MessageDigest))


;; DIGEST ALGORITHMS

(def ^:private algorithm-names
  "Map of content hashing algorithms to system names."
  {:md5    "MD5"
   :sha1   "SHA-1"
   :sha256 "SHA-256"})


(def algorithms
  "Set of available content hashing algorithms."
  (set (keys algorithm-names)))


(def ^:dynamic *digest-algorithm*
  "Default digest algorithm to use for content hashing."
  :sha256)


(defmacro with-algorithm
  "Executes a body of expressions with the given default digest algorithm."
  [algorithm & body]
  `(binding [*digest-algorithm* ~algorithm]
     ~@body))


(defn- check-algorithm
  "Throws an exception if the given keyword is not a valid algorithm
  identifier."
  [algo]
  (when-not (algorithms algo)
    (throw (IllegalArgumentException.
             (str "Unsupported digest algorithm: " algo
                  ", must be one of: " (string/join " " algorithms))))))



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

; FIXME: figure out where to put this
;(data/extend-tagged-str BlobRef vault/ref)


(defn parse-id
  "Parses a hash identifier string into a blobref. Accepts either a hash URN
  or the shorter \"algo:digest\" format."
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
     (check-algorithm algo)
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


(defn- hex-signature
  "Formats a sequence of bytes into a hexadecimal string."
  [^bytes digest]
  (let [length (* 2 (count digest))
        hex (-> (BigInteger. 1 digest)
                (.toString 16)
                (.toLowerCase))
        padding (apply str (repeat (- length (count hex)) "0"))]
    (str padding hex)))


(defn hash
  "Calculates the hash digest of the given data source. Returns a HashID."
  [algo content]
  (check-algorithm algo)
  (let [algorithm (MessageDigest/getInstance (algorithm-names algo))
        data-seq (map byte-streams/to-byte-array
                      (byte-streams/to-byte-buffers content))]
    (doseq [^bytes data data-seq]
      (.update algorithm data))
    (->HashID algo (hex-signature (.digest algorithm)))))



;; BLOB DATA

(defrecord BlobData
  [id content])


(defn- buffer-data
  ^ByteBuffer
  [source]
  (let [^ByteBuffer buffer (byte-streams/convert source ByteBuffer)]
    (.asReadOnlyBuffer buffer)))



;; STORAGE INTERFACE

(defprotocol BlobStore
  "Protocol for content storage providers, keyed by hash ids."

  (-list [this opts]
    "Enumerates the stored blobs with some filtering options.")

  (-stat [this id]
    "Returns the stored status metadata for a blob.")

  (-open [this id]
    "Returns a an open input stream of the stored data.")

  (-store! [this blob]
    "Persists blob content in the store.")

  (-remove! [this id]
    "Returns true if the store contained the blob."))


(defn list
  "Enumerates the stored blobs, returning a sequence of HashIDs.
  Options should be keyword/value pairs from the following:
  * :after  - start enumerating ids lexically following this string
  * :prefix - only return ids matching the given string
  * :limit  - limit the number of results returned"
  ([store]
   (-list store nil))
  ([store opts]
   (-list store opts))
  ([store opt-key opt-val & opts]
   (-list store (apply hash-map opt-key opt-val opts))))


(defn stat
  "Returns a map of metadata about the blob, if it is stored. Properties are
  implementation-specific, but may include:
  * :size         - blob size in bytes
  * :since        - date blob was added to store
  * :location     - a resource location for the blob"
  [store id]
  (-stat store id))


(defn contains?
  "Determines whether the store contains the referenced blob."
  [store id]
  (not (nil? (-stat store id))))


(defn open
  "Opens a stream of byte content for the referenced blob, if it is stored."
  ^java.io.InputStream
  [store id]
  (-open store id))


(defn store!
  "Stores data from the given byte source and returns the blob's hash id."
  [store source]
  (let [content (buffer-data source)
        id (hash *digest-algorithm* content)]
    (-store! store (->BlobData id content))
    id))


(defn remove!
  "Remove the referenced blob from this store. Returns true if the store
  contained the blob when this method was called."
  [store id]
  (-remove! store id))
