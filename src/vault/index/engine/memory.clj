(ns vault.index.engine.memory
  (:require
    [puget.order :as order]
    [vault.index.search :as search]))


;;;;; HELPER METHODS ;;;;;

(defn- flatten-times
  "Flattens a sorted map into a sequence of values n times."
  [m n]
  (apply concat (nth (iterate (partial mapcat vals) [m]) n)))


(defn- update-sorted-in
  "Like update-in, but creates intermediate maps sorted by Puget."
  [m [k & ks] f & args]
  (if ks
    (assoc m k (apply update-sorted-in (get m k (sorted-map-by order/rank)) ks f args))
    (assoc m k (apply f (get m k) args))))


(defn- update-record
  "Updates a nested index map with the given record. Creates a sorted set as
  the leaf value if one is not present already."
  [index ks record]
  (update-sorted-in index ks (fnil conj (sorted-set-by order/rank)) record))



;;;;; MEMORY INDEX ;;;;;

(defrecord MemoryIndex [attrs index])


(extend-type MemoryIndex
  search/Engine

  (search
    [this pattern opts]
    (loop [[attr & more :as attrs] (:attrs this)
           index (deref (:index this))
           pattern pattern]
      ; Find leading attributes present in `pattern`.
      (if-let [value (get pattern attr)]
        ; Narrow scope by recursively getting next level of indexes.
        (recur more (get index value) (dissoc pattern attr))
        ; Filter remaining entries by pattern attributes.
        (filter (partial search/matches? pattern)
                (flatten-times index (count attrs))))))

  (update!
    [{:keys [attrs index] :as this} record]
    (let [key-vec (vec (map record attrs))]
      (when (some nil? key-vec)
        (throw (IllegalArgumentException.
                 (str "Cannot update index with record missing required "
                      "attributes " (pr-str attrs) " " (pr-str record)))))
      (swap! (:index this) update-record key-vec record))))


(defn memory-index
  [& attrs]
  {:pre [(seq attrs)]}
  (MemoryIndex. (vec attrs) (atom (sorted-map))))
