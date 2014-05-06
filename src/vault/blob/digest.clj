(ns vault.blob.digest
  (:refer-clojure :exclude [hash])
  (:require
    [clojure.string :as str])
  (:import
    java.security.MessageDigest))


;; DIGEST ALGORITHMS

(def algorithms
  "Map of content hashing algorithms to system names."
  {:md5    "MD5"
   :sha1   "SHA-1"
   :sha256 "SHA-256"})


(def ^:dynamic *algorithm*
  "Default digest algorithm to use for content hashing."
  :sha256)



;; HASH IDENTIFIER

(defrecord HashID
  [algorithm ^String digest]

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
    (HashID. algorithm digest)))


(defn hash-id
  "Constructs a hash identifier from the arguments."
  ([x]
   (cond
     (instance? HashID x) x
     :else (parse-id (str x))))
  ([algorithm digest]
   (HashID. (keyword algorithm) (str digest))))



;; CONTENT HASHING

(defn- zero-pad
  "Pads a string with leading zeroes up to the given width."
  [width value]
  (let [string (str value)]
    (if (<= width (count string))
      string
      (-> width
          (- (count string))
          (repeat "0")
          str/join
          (str string)))))


(defn- hex-str
  "Converts a byte array into a lowercase hex string."
  [^bytes value]
  (let [width (* 2 (count value))
        hex (-> (BigInteger. 1 value)
                (.toString 16)
                str/lower-case)]
    (zero-pad width hex)))


(defn hash
  "Calculates the digest of the given byte array and returns a HashID."
  ([content]
   (hash *algorithm* content))
  ([algorithm ^bytes content]
   {:pre [(contains? algorithms algorithm)]}
   (let [hex-digest (-> (algorithms algorithm)
                        MessageDigest/getInstance
                        (.digest content)
                        hex-str)]
     (HashID. algorithm hex-digest))))
