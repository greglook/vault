(ns vault.digest
  (:require [clojure.string :as string]
            digest))


;; CONTENT HASHING

(def ^:private hashing-functions
  "Map of content hashing algorithms to functional implementations."
  {:md5    digest/md5
   :sha1   digest/sha-1
   :sha256 digest/sha-256})


(def algorithms
  "Set of available content hashing algorithms."
  (set (keys hashing-functions)))


(def ^:dynamic *algorithm*
  "Default digest algorithm to use for content hashing."
  :sha256)


(defn assert-valid-digest
  [algorithm]
  (when-not (hashing-functions algorithm)
    (throw (IllegalArgumentException.
             (str "Unsupported digest algorithm: " algorithm
                  ", must be one of: " (string/join ", " algorithms))))))


(defmacro with-algorithm
  "Executes a body of expressions with the given default digest algorithm."
  [algorithm & body]
  `(binding [*algorithm* ~algorithm]
     (assert-valid-digest *algorithm*)
     ~@body))


(defn digest-stream
  "Wraps the given output stream in ..."
  )


(defn digest
  "Calculates the hash of the given content."
  ([content]
   (digest *algorithm* content))
  ([algorithm content]
   (assert-valid-digest algorithm)
   (let [hashfn (hashing-functions algorithm)
         digest ^String (hashfn content)]
     (.toLowerCase digest))))
