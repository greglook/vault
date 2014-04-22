(ns vault.data.core
  (:require
    (vault.data.format
      [edn :as edn-data]
      [pgp :as pgp-data])
    [vault.data.signature :as sig]))


(defn read-blob
  "Reads the contents of the given blob and attempts to parse its structure.
  Returns an updated copy of the blob with at least a :data/type key set."
  [blob]
  (if (:data/type blob)
    ; If blob has a data type, assume it's already been processed.
    blob
    ; Try to read the content as EDN data.
    (if-let [data-blob (edn-data/read-blob blob)]
      ; Try verifying the signatures (if any) in the blob.
      (sig/verify data-blob)
      ; Otherwise, check if it is a PGP object, if not call it a binary blob.
      (or (pgp-data/read-blob blob)
          (assoc blob :data/type :bytes)))))
