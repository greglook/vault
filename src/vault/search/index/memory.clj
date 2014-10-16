(ns vault.search.index.memory
  "Memory indexes store records in a sorted map."
  (:require
    [puget.order :as order]
    [vault.search.index :as index]))


;; A memory index uses a sorted map contained in an atom to store records. Each
;; record is keyed by a sequence of attributes, which determines the order in
;; which they are stored.
(defrecord MemoryIndex
  [unique-key records]

  index/SortedIndex

  (seek
    [this components]
    (if components
      (let [start-key (index/key-vec unique-key components)]
        (map val (subseq @records >= start-key)))
      (vals @records)))


  (insert!
    [this record]
    {:pre [(map? record)]}
    (let [pkey (index/key-vec unique-key record)]
      (swap! records assoc pkey record))
    this)


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
