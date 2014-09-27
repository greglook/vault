(ns vault.search.index.memory
  (:require
    [puget.order :as order]
    [vault.search.index :as index]))


(defrecord MemoryIndex
  [schema tuples]

  index/Index

  (insert!
    [this record]
    {:pre [(map? record)]}
    (swap! tuples assoc (index/key-vec schema record) record)
    this)

  (erase!!
    [this]
    (swap! tuples empty)
    nil)


  index/SortedIndex

  (seek*
    [this start end ascending]
    (map val
      (if ascending
        (if end
          (subseq @tuples
            >= (index/key-vec schema start)
            <  (index/key-vec schema end))
          (subseq @tuples
            >= (index/key-vec schema start)))
        (if end
          (rsubseq @tuples
            >  (index/key-vec schema end)
            <= (index/key-vec schema start))
          (rsubseq @tuples
            <= (index/key-vec schema start)))))))


(extend-type MemoryIndex
  index/KeyValueIndex

  (get
    [{:keys [schema tuples]} key]
    {:pre [(map? key)]}
    (get @tuples (index/key-vec schema key)))

  (delete!
    [{:keys [schema tuples]} key]
    {:pre [(map? key)]}
    (swap! tuples dissoc (index/key-vec schema key))
    nil))


(defn memory-index
  "Creates a new memory-backed index."
  [& schema]
  (MemoryIndex.
    (vec schema)
    (atom (sorted-map-by order/rank))))
