(ns vault.data.struct
  "Base functions for handling data attributes on blobs.

  The two common data attributes include:
  - `:data/type`    keyword giving type of content stored (`:pgp/public-key`, `:raw`, other edn type)
  - `:data/values`  vector of data values deserialized from the content"
  {:doc/format :markdown})


(defn data-attrs
  "Adds attributes about the data in the blob content."
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
