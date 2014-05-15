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
    typed-map)
  (vault.data.crypto
    sign-value
    verify-sigs))


(defn read-blob
  "Reads the contents of the given blob and attempts to parse its structure.
  Returns an updated copy of the blob with at least a :data/type key set."
  [store blob]
  (if (:data/type blob)
    ; If blob has a data type, assume it's already been processed.
    blob
    ; Try to read the content as EDN data.
    (if-let [data-blob (edn-data/read-blob blob)]
      ; Try verifying the signatures (if any) in the blob.
      (crypto/verify-sigs data-blob store)
      ; Otherwise, check if it is a PGP object, if not call it a binary blob.
      (or (pgp-data/read-blob blob)
          (assoc blob :data/type :raw)))))
