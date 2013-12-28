(ns vault.util.digest
  (:require
    byte-streams
    [vault.util.io :refer [do-bytes]])
  (:import
    java.security.MessageDigest))


;; DIGEST ALGORITHMS

(def algorithms
  "Map of content hashing algorithms to system names."
  {:md5    "MD5"
   :sha1   "SHA-1"
   :sha256 "SHA-256"})


(defn check-algorithm
  "Throws an exception if the given keyword is not a valid algorithm
  identifier."
  [algo]
  (when-not (algorithms algo)
    (throw (IllegalArgumentException.
             (str "Unsupported digest algorithm: " algo
                  ", must be one of: " algorithms)))))



;; CONTENT HASHING

(defn- hex-signature
  "Formats a sequence of bytes into a hexadecimal string."
  [^bytes digest]
  (let [length (* 2 (count digest))
        hex (-> (BigInteger. 1 digest)
                (.toString 16)
                (.toLowerCase))
        padding (apply str (repeat (- length (count hex)) "0"))]
    (str padding hex)))


(defn hash-content
  "Calculates the hash digest of the given data source. Returns the digest as
  a hex string."
  [algo content]
  (check-algorithm algo)
  (let [algorithm (MessageDigest/getInstance (algorithms algo))]
    (.reset algorithm)
    (do-bytes content #(.update algorithm %1 0 %2))
    (hex-signature (.digest algorithm))))
