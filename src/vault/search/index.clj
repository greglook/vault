(ns vault.search.index
  "Indexes store a collection of _records_ which can be searched for values
  matching a pattern."
  (:require
    [puget.order :as order]
    [schema.core :as schema]))


(defprotocol Index
  "Protocol for an index which can collect records."

  (search*
    [index query]
    "Searches the index for an ordered sequence of records. See `search`.")

  (insert!
    [index record]
    "Updates the index by inserting a new record value.")

  (delete!
    [index pattern]
    "Removes records from the index which match the given pattern.")

  (erase!!
    [index]
    "Removes all records stored in the index."))


(defn put!
  "Stores a blob in the index by projecting it into records. The index must
  contain a `:projection` key with a function which returns a sequence of
  records for an input blob."
  [index blob]
  (when-let [projection (:projection index)]
    (doseq [record (projection blob)]
      (insert! index record)))
  blob)


(defn search
  "Returns a (potentially lazy) sequence of records from the index. The order
  in which records are returned is up to the underlying implementation. Indexes
  may store records internally in different orders to optimize different query
  patterns.

  - `:where` may be a map of keywords to values which will be checked against
    the stored records
  - `:order` may specify a keyword or vector of keywords to sort the returned
    results by

  If the index has a `:schema` key set, the returned records will be validated
  before they are returned."
  ([index]
   (search index nil))
  ([index query]
   (let [schema (:schema index)]
     (cond->> (search* index query)
       schema (map #(do (schema/validate schema %) %)))))
  ([index query-key query-val & more]
   (search index (apply hash-map query-key query-val more))))



;; ## Index Functions

(defn ^:no-doc key-vec
  "Extracts a key vector from a record and a sequence of keywords."
  [key-attrs record]
  {:pre [(vector? key-attrs) (every? keyword? key-attrs)]}
  (vec (map (partial clojure.core/get record) key-attrs)))


(defn ^:no-doc matches?
  "Determines whether a record matches the given pattern."
  [pattern record]
  (every? #(= (get pattern %)
              (get record %))
          (keys pattern)))


(defn ^:no-doc filter-records
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
