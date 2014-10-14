(ns vault.search.index
  "Indexes store a collection of _records_ which can be searched for values
  matching a pattern."
  (:refer-clojure :exclude [find])
  (:require
    [vault.data.struct :as struct]))


(defprotocol SortedIndex
  "Protocol for an index of data which stores efficiently-searchable records."

  (seek
    [index components]
    "Provides raw access to the index data as a (potentially lazy) sequence.
    Optionally, one or more leading components of the index can be supplied as
    a map of keys to component values.

    Note that there need not be an exact match on the supplied components. The
    sequence will begin at or after the point in the index where the components
    would reside. Further, the sequence is not bound by the supplied
    components, and will only terminate at the end of the index.")

  (insert!
    [index record]
    "Updates the index by inserting a new record.")

  (erase!!
    [index]
    "Removes all records stored in the index."))


(defn matches?
  "Returns true if the record matches the given pattern. For each key in the
  pattern, the value must match the value stored in the record."
  [pattern record]
  (every? #(= (get pattern %)
              (get record %))
          (keys pattern)))


(defn find
  "Searches for records matching the given pattern in the index."
  [index pattern]
  (filter (partial matches? pattern)
          (seek index pattern)))



;; ## Record Projection

(defn key-vec
  "Extracts a key vector from a record and a sequence of keywords."
  [key-attrs record]
  {:pre [(vector? key-attrs) (every? keyword? key-attrs)]}
  (vec (map (partial get record) key-attrs)))


(defn filter-types
  "Wraps a projection function in a check that the blob's data type passes the
  given predicate."
  [projection pred]
  {:pre [(fn? projection)]}
  (fn [blob]
    (when (pred (struct/data-type blob))
      (projection blob))))


(defn put!
  "Stores a blob in the index by projecting it into records. The index must
  contain a `:projection` key with a function which returns a sequence of
  records for an input blob."
  [index blob]
  (when-let [projection (:projection index)]
    (doseq [record (projection blob)]
      (insert! index record)))
  blob)
