(ns vault.blob.digest
  (:refer-clojure :exclude [hash])
  (:require
    byte-streams
    [clojure.string :as string])
  (:import
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


(defn- check-algorithm
  "Throws an exception if the given keyword is not a valid algorithm
  identifier."
  [algo]
  (when-not (contains? algorithms algo)
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


(defn hash
  "Calculates the hash digest of the given byte array. Returns a HashID."
  [algo content]
  (check-algorithm algo)
  (let [algorithm (MessageDigest/getInstance (algorithm-names algo))
        length (* 2 (.getDigestLength algorithm))
        data (byte-streams/to-byte-array content)
        digest (.digest algorithm data)
        hex (-> (BigInteger. 1 digest) (.toString 16) .toLowerCase)
        padding (apply str (repeat (- length (count hex)) "0"))]
    (->HashID algo (str padding hex))))
