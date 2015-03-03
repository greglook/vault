(ns vault.data.bytes
  "The functions in this namespace provide facilities for handling byte
  sequences in Vault."
  (:require
    [clojure.data.codec.base64 :as b64]
    [vault.data.edn :as edn]))


(defn read-bin
  "Reads a base64-encoded string into a byte array. Suitable as a data-reader
  for `bytes/bin` literals."
  ^bytes
  [^String bin]
  (b64/decode (.getBytes bin)))


(edn/register-tag! bytes/bin
  (class (byte-array 0))
  #(->> % b64/encode (map char) (apply str))
  read-bin)
