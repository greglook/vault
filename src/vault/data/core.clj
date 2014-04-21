(ns vault.data.core
  (:require
    (vault.data.format
      [edn :as edn-data]
      [pgp :as pgp-data])))


(defn read-blob
  "Reads the contents of the given blob and attempts to parse its structure.
  Returns an updated copy of the blob with at least a :data/type key set."
  [blob]
  (if (:data/type blob)
    ; If blob has a data type already, assume it's already been processed.
    blob
    ; Otherwise, try to read it as edn, then pgp, then just call it a binary blob.
    (or (edn-data/read-blob blob)
        (pgp-data/read-blob blob)
        (assoc blob :data/type :bytes))))
