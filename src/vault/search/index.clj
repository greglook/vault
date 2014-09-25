(ns vault.search.index
  (:refer-clojure :exclude [get]))


(defprotocol TupleIndex
  "Protocol for an index of vector tuples. Records are identified by keys, which
  are stored in sorted order."

  (seek*
    [this start end ascending]
    "Searches the index for records following the given start key. If ascending
    is true, records which are 'greater than or equal to' the start pattern are
    returned. Otherwise, results are returned in reverse order.")

  (insert!
    [this record]
    "Updates the index by inserting a record value. Should return the index.")

  (delete!
    [this record]
    "Removes a record from the index for the given key map."))


(defn seek
  "Searches the index for records following the given key. If ascending is
  true, records which are 'greater than or equal to' the start pattern are
  returned. Otherwise, results are returned in reverse order." 
  ([index]
   (seek* index nil nil true))
  ([index start]
   (seek* index start nil true))
  ([index start ascending]
   (seek* index start nil ascending))
  ([index start end ascending]
   (seek* index start end ascending)))


(defn key-vec
  "Extracts a key vector from a record and a schema, a sequence of keywords."
  [schema record]
  {:pre [(vector? schema) (every? keyword? schema)]}
  (vec (map (partial clojure.core/get record) schema)))


(defn get
  "Retrieves a record matching the key."
  [index key]
  (first (seek* index key nil true)))
