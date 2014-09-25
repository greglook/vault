(ns vault.search.index.memory
  (:require
    [puget.order :as order]
    [vault.search.index :as index]))


(defrecord MemoryIndex
  [schema tuples]

  index/TupleIndex

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
            <= (index/key-vec schema start)
            >  (index/key-vec schema end))
          (rsubseq @tuples
            <= (index/key-vec schema start))))))


  (insert!
    [this record]
    {:pre [(map? record)]}
    (swap! tuples assoc (index/key-vec schema record) record)
    this)


  (delete!
    [this record]
    {:pre [(map? key)]}
    (swap! tuples dissoc (index/key-vec schema record))
    nil))


(defn memory-index
  "Creates a new memory-backed index."
  [& schema]
  (MemoryIndex.
    (vec schema)
    (atom (sorted-map-by order/rank))))
