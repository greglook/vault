(ns vault.blob.hash
  (:require
    [clojure.string :as str])
  (:import
    java.security.MessageDigest))


(def algorithms
  "Map of content hashing algorithms to system names."
  {:md5    "MD5"
   :sha1   "SHA-1"
   :sha256 "SHA-256"})


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


(defn digest
  "Calculates the hash digest of the given byte array. Returns a lowercase
  hexadecimal string."
  [algo ^bytes content]
  {:pre [(contains? algorithms algo)]}
  (->
    (algorithms algo)
    MessageDigest/getInstance
    (.digest content)
    hex-str))
