(ns vault.index.engine.memory
  (:require
    [puget.order :as order]
    [vault.index.engine :as engine]))


;;;;; HELPER FUNCTIONS ;;;;;

(defn- update-vals
  "Maps a function over the values in a map, returning a new map."
  [m f & args]
  (reduce (fn [acc [k v]] (assoc acc k (apply f v args))) (empty m) m))


(defn- flatten-times
  "Flattens a sorted map into a sequence of values n times."
  [m n]
  (apply concat (nth (iterate (partial mapcat vals) [m]) n)))


(defn- update-sorted-in
  "Like update-in, but creates intermediate maps sorted by Puget."
  [m [k & ks] f & args]
  (if ks
    (assoc m k (apply update-sorted-in
                      (get m k (sorted-map-by order/rank))
                      ks f args))
    (assoc m k (apply f (get m k) args))))



;;;;; REGISTER METHODS ;;;;;

(defn- mem-register
  "Creates a new register map with the given attributes."
  [attrs]
  {:attrs (vec attrs)
   :records (sorted-map-by order/rank)})


(defn- update-register
  "Updates a nested register map with the given record. Creates a sorted set as
  the leaf value if one is not present already. Returns an updated version of
  the register."
  [register record]
  (let [key-vec (vec (map record (:attrs register)))]
    (when (some nil? key-vec)
      (throw (IllegalArgumentException.
               (str "Cannot update register with record missing required "
                    "attributes " (pr-str (:attrs register)) " "
                    (pr-str record)))))
    (update-in register [:records]
               update-sorted-in key-vec
               (fnil conj (sorted-set-by order/rank))
               record)))


(defn- select-register
  "Given a collection of registers and attributes being searched, select the
  best register to query for records."
  [registers attrs]
  (let [leading-attrs
        #(->> % :attrs
              (map (set attrs))
              (take-while some?)
              count)]
    (apply max-key leading-attrs registers)))


(defn- search-register
  "Queries a register to find records matching the given pattern. Returns a
  sequence of the matched records."
  [register pattern opts]
  (loop [[attr & more :as attrs] (:attrs register)
         records (:records register)
         pattern pattern]
    ; Find leading attributes present in `pattern`.
    (if-let [value (get pattern attr)]
      ; Narrow scope by recursively getting next level of records.
      (recur more (get records value) (dissoc pattern attr))
      ; Filter remaining entries by pattern attributes.
      (filter (partial engine/matches? pattern)
              (flatten-times records (count attrs))))))



;;;;; MEMORY INDEX ;;;;;

(defrecord MemoryIndex [registers])

(extend-type MemoryIndex
  engine/SearchEngine

  (init!
    [this]
    ; no-op
    this)

  (update!
    [this record]
    (swap! (:registers this) update-vals update-register record)
    this)

  (search*
    [this pattern opts]
    (-> this :registers deref vals
        (select-register (keys pattern))
        (search-register pattern opts))))


(defn memory-index
  "Creates a new memory-backed index which optimizes the given queries. The
  argument should be a map of query keyword names to attr vectors."
  [queries]
  (MemoryIndex. (atom (update-vals queries mem-register))))
