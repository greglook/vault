(ns mvxcvi.crypto.digest
  "Utility functions for producing hash digests."
  (:require
    byte-streams
    [clojure.string :as str]
    [mvxcvi.crypto.util :refer [do-bytes hex-str]])
  (:import
    java.security.MessageDigest))


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


(defn hash-content
  "Calculates the hash digest of the given data source. Returns the digest as
  a hex string."
  [algo content]
  (check-algorithm algo)
  (let [algorithm (MessageDigest/getInstance (algorithms algo))]
    (.reset algorithm)
    (do-bytes [[buf n] content]
      (.update algorithm buf 0 n))
    (hex-str (.digest algorithm))))
