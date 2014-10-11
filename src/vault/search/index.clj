(ns vault.search.index
  "Indexes store a collection of _records_ which can be searched for values
  matching a pattern."
  (:refer-clojure :exclude [find])
  (:require
    [puget.order :as order]
    [schema.core :as schema]
    [vault.data.struct :as struct]))


(defprotocol Index
  "Protocol for an index which stores efficiently-searchable records."

  (get*
    [index key]
    "Looks up a record directly by key.")

  (seek*
    [index order components]
    "Seeks into the index with a certain ordering. See `seek`.")

  (insert!
    [index record]
    "Updates the index by inserting a new record.")

  (delete!
    [index key]
    "Removes records from the index which match the given key.")

  (erase!!
    [index]
    "Removes all records stored in the index."))


(defn seek
  "Raw access to the index data, by index. The index must be supplied, and,
  optionally, one or more leading components of the index can be supplied for
  the initial search. Note that there need not be an exact match on the
  supplied components. The iteration will begin at or after the point in the
  index where the components would reside. Further, the iteration is not bound
  by the supplied components, and will only terminate at the end of the index."
  [index order & components]
  (seek* index order components))


(defn find
  "Search for records matching the given pattern."
  [index order & components]
  nil)


(defn put!
  "Stores a blob in the index by projecting it into records. The index must
  contain a `:projection` key with a function which returns a sequence of
  records for an input blob."
  [index blob]
  (when-let [projection (:projection index)]
    (doseq [record (projection blob)]
      (insert! index record)))
  blob)



;; ## Index Functions

(defn key-vec
  "Extracts a key vector from a record and a sequence of keywords."
  [key-attrs record]
  {:pre [(vector? key-attrs) (every? keyword? key-attrs)]}
  (vec (map (partial clojure.core/get record) key-attrs)))


(defn matches?
  "Returns true if the record matches the given pattern. For each key in the
  pattern, the value must match the value stored in the record."
  [pattern record]
  (every? #(= (get pattern %)
              (get record %))
          (keys pattern)))


(defn filter-types
  "Wraps a projection function in a check that the blob's data type passes the
  given predicate."
  [projection pred]
  {:pre [(fn? projection)]}
  (fn [blob]
    (when (pred (struct/data-type blob))
      (projection blob))))


(defn filter-records
  "Filters and sorts a returned sequence of records to respect the query's
  `:where` and `:order` clauses."
  [query records]
  (let [pattern (:where query)
        order-key (if-let [ordering (:order query)]
                    (if (keyword? ordering)
                      (comp vector ordering)
                      (apply juxt ordering)))]
    (cond->> records
      pattern   (filter (partial matches? (:where query)))
      order-key (sort-by order-key order/rank))))
