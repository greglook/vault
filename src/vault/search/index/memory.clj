(ns vault.search.index.memory
  "Memory indexes store records in a sorted map."
  (:require
    [puget.order :as order]
    [vault.search.index :as index]))


;; A memory index uses a sorted map contained in an atom to store records. Each
;; record is keyed by a sequence of attributes, which determines the order in
;; which they are stored.
(defrecord MemoryIndex
  [records unique-key]

  index/Index

  #_
  (search*
    [this query]
    (index/filter-records
      query
      (if-let [start (some->> query :where (index/key-vec unique-key))]
        (map val (subseq @records >= start))
        (vals @records))))


  (insert!
    [this record]
    {:pre [(map? record)]}
    (swap! records assoc (index/key-vec unique-key record) record)
    this)


  (delete!
    [this pattern]
    {:pre [(map? pattern)]}
    (swap! records dissoc (index/key-vec unique-key pattern))
    nil)


  (erase!!
    [this]
    (swap! records empty)
    nil))


(defn memory-index
  "Creates a new memory-backed index keyed on attributes in `:unique-key`. The
  records inserted will be ordered by their values for those attributes."
  [opts]
  {:pre [(contains? opts :unique-key)
         (pos? (count (:unique-key opts)))]}
  (assoc
    (map->MemoryIndex opts)
    :records (atom (sorted-map-by order/rank))))
