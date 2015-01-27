(ns vault.data.struct
  "Base functions for handling data attributes on blobs. The two common data
  attributes include:

  - `:data/type`    keyword giving type of content stored
      - `:raw` for generic binary data
      - `:pgp/public-key` for PGP public keys
      - other EDN value type like `:vault.entity/root`
  - `:data/values` a vector of values deserialized from the content")


(def ^:const type-key
  "Keyword in a blob record which stores the type of the contained data."
  :data/type)


(def ^:const values-key
  "Keyword in a blob record which stores the type of the contained data."
  :data/values)


(defn data-attrs
  "Associates data attributes with the blob content."
  [blob data-type values & ks]
  (apply assoc blob
    type-key data-type
    values-key (vec values)
    ks))


(defn data-type
  "Returns the type of data stored in the blob."
  [blob]
  (type-key blob))


(defn data-value
  "Returns the first data value from the blob."
  [blob]
  (first (values-key blob)))


#_
(defn parse-blob
  "Reads the contents of the given blob and attempts to parse its structure.
  Returns an updated copy of the blob with a :data/type key set."
  [blob]
  ; If blob has a data type, assume it's already been processed.
  (if (type-key blob)
    blob
    (or (edn/parse-data blob)
        (key/parse-key blob)
        (assoc blob type-key :raw))))
