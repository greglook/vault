(ns vault.blob.digest
  (:refer-clojure :exclude [hash])
  (:require
    [clojure.string :as string])
  (:import
    (java.security
      MessageDigest)))


(def ^:private algorithm-names
  "Map of content hashing algorithms to functional implementations."
  {:md5    "MD5"
   :sha1   "SHA-1"
   :sha256 "SHA-256"})


(def algorithms
  "Set of available content hashing algorithms."
  (set (keys algorithm-names)))


(def ^:dynamic *algorithm*
  "Default digest algorithm to use for content hashing."
  :sha256)


(defn check-algorithm
  "Throws an exception if the given keyword is not a valid algorithm
  identifier."
  [id]
  (when-not (contains? algorithms id)
    (throw (IllegalArgumentException.
             (str "Unsupported digest algorithm: " id
                  ", must be one of: " (string/join " " algorithms))))))


(defmacro with-algorithm
  "Executes a body of expressions with the given default digest algorithm."
  [algorithm & body]
  `(binding [*algorithm* ~algorithm]
     (check-algorithm *algorithm*)
     ~@body))


(defn hash
  "Calculates the hash digest of the given byte array. Returns a vector of the
  algorithm id and the hash hex string."
  ([data]
   (hash *algorithm* data))
  ([id data]
   (check-algorithm id)
   (let [algorithm (MessageDigest/getInstance (algorithm-names id))
         length (* 2 (.getDigestLength algorithm))
         digest (.digest algorithm data)
         hex (-> (BigInteger. 1 digest) (.toString 16) .toLowerCase)
         padding (apply str (repeat (- length (count hex)) "0"))]
     [id (str padding hex)])))
