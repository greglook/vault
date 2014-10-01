(ns vault.search.index.memory
  "Memory indexes store records in a sorted map."
  (:require
    [puget.order :as order]
    [vault.search.index :as index]))


;; A memory index uses a sorted map contained in an atom to store records. Each
;; record is keyed by a sequence of attributes, which determines the order in
;; which they are stored.
(defrecord MemoryIndex
  [key-attrs tuples]

  index/Index

  (search*
    [this query]
    (index/filter-records
      query
      (if-let [start (some->> query :where (index/key-vec key-attrs))]
        (map val (subseq @tuples >= start))
        (vals @tuples))))


  (insert!
    [this record]
    {:pre [(map? record)]}
    (swap! tuples assoc (index/key-vec key-attrs record) record)
    this)


  (delete!
    [this pattern]
    {:pre [(map? pattern)]}
    (swap! tuples dissoc (index/key-vec key-attrs pattern))
    nil)


  (erase!!
    [this]
    (swap! tuples empty)
    nil))


(defn memory-index
  "Creates a new memory-backed index keyed on the given attributes. The records
  inserted will be ordered by their values for those attributes."
  [& key-attrs]
  {:pre [(<= 1 (count key-attrs))]}
  (MemoryIndex.
    (vec key-attrs)
    (atom (sorted-map-by order/rank))))
