(ns vault.data.core
  (:require
    (vault.data
      [edn :as edn]
      [key :as key])))


(defn parse-blob
  "Reads the contents of the given blob and attempts to parse its structure.
  Returns an updated copy of the blob with a :data/type key set."
  [blob]
  ; If blob has a data type, assume it's already been processed.
  (if (:data/type blob)
    blob
    (or (edn/parse-data blob)
        (key/parse-key blob)
        (assoc blob :data/type :raw))))


(defn blob-type
  "Determines the type of content stored in the blob."
  [blob]
  (:data/type blob))


(defn blob-value
  "Extracts the first data value from the given blob."
  [blob]
  (first (:data/values blob)))
