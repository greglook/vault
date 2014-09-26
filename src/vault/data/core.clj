(ns vault.data.core
  (:refer-clojure :exclude [type])
  (:require
    [potemkin :refer [import-vars]]
    (vault.data
      [edn :as edn]
      [key :as key]
      signature)))


(import-vars
  (vault.data.edn
    type
    type-key
    typed-map)
  (vault.data.signature
    sign-value
    verify-sigs))


(defn parse-blob
  "Reads the contents of the given blob and attempts to parse its structure.
  Returns an updated copy of the blob with a :data/type key set."
  [blob]
  ; If blob has a data type, assume it's already been processed.
  (if (:data/type blob)
    blob
    (or (edn/parse-blob blob)
        (key/parse-blob blob)
        (assoc blob :data/type :raw))))


(defn blob-value
  "Extracts the first data value from the given blob."
  [blob]
  (first (:data/values blob)))
