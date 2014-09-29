(ns vault.data.struct
  "Base functions for handling data attributes on blobs. The two common data
  attributes include:

  - `:data/type`    keyword giving type of content stored
      - `:raw` for generic binary data
      - `:pgp/public-key` for PGP public keys
      - other EDN value type like `:vault.entity/root`
  - `:data/values` a vector of values deserialized from the content")


(defn data-attrs
  "Associates data attributes with the blob content."
  [blob data-type values & ks]
  (apply assoc blob
    :data/type data-type
    :data/values (vec values)
    ks))


(defn data-type
  "Returns the type of data stored in the blob."
  [blob]
  (:data/type blob))


(defn data-value
  "Returns the first data value from the blob."
  [blob]
  (first (:data/values blob)))


#_
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
