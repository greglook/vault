(ns vault.data.core
  (:refer-clojure :exclude [type])
  (:require
    [potemkin :refer [import-vars]]
    (vault.data
      [edn :as edn-data]
      [pgp :as pgp-data]
      [crypto :as crypto])))


(import-vars
  (vault.data.edn
    type
    type-key
    typed-map)
  (vault.data.crypto
    sign-value
    verify-sigs))


(defn read-blob
  "Reads the contents of the given blob and attempts to parse its structure.
  Returns an updated copy of the blob with a :data/type key set."
  [blob]
  ; If blob has a data type, assume it's already been processed.
  (if (:data/type blob)
    blob
    (or (edn-data/read-blob blob)
        (pgp-data/parse-blob blob)
        (assoc blob :data/type :raw))))


(defn blob-value
  "Extracts the first data value from the given blob."
  [blob]
  (first (:data/values blob)))
