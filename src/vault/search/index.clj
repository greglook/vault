(ns vault.search.index
  ""
  (:refer-clojure :exclude [get]))


(defprotocol Index
  "Protocol for a generic index which can collect records."

  (insert!
    [index record]
    "Updates the index by inserting a record value.")

  (erase!!
    [index]
    "Removes all records stored in the index."))


(defprotocol KeyValueIndex
  "Protocol for storing records uniquely identified by a key."

  (get
    [index key]
    "Returns the record stored for the given key.")

  (delete!
    [index key]
    "Removes a record from the index for the given key."))


(defprotocol SortedIndex
  "Protocol for a sorted index of vector tuples. Records are identified by keys, which
  are stored in sorted order."

  (seek*
    [index start end ascending]
    "Searches the index for records following the given start record. If
    ascending is true, records which are 'greater than or equal to' the start
    are returned. Otherwise, results are returned in reverse order."))


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


(defn ^:no-doc key-vec
  "Extracts a key vector from a record and a schema, a sequence of keywords."
  [schema record]
  {:pre [(vector? schema) (every? keyword? schema)]}
  (vec (map (partial clojure.core/get record) schema)))
