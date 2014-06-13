(ns vault.index.core
  (:require
    [puget.order :as order]
    [vault.blob.store :as store :refer [BlobStore]]))


(defprotocol Index
  "Protocol for a single index."

  (search
    [this pattern opts]
    "Search the index for records with values matching the given pattern.
    Options may include:
    - :ascending   Whether to return the sequence in ascending order.")

  (update
    [this record]
    "Return an updated version of the index with information about the record.
    Immutability of old index values is NOT guaranteed."))


;; HELPER METHODS

(defn update-sorted-in
  "Like update-in, but creates intermediate maps sorted by Puget."
  [m [k & ks] f & args]
  (if ks
    (assoc m k (apply update-sorted-in (get m k (sorted-map-by order/rank)) ks f args))
    (assoc m k (apply f (get m k) args))))



;; MEMORY INDEX

(defrecord MemoryIndex [attrs index])


(extend-type MemoryIndex
  Index

  (search
    [this pattern opts]
    (loop [[attr & more :as attrs] (:attrs this)
           index (:index this)
           pattern pattern]
      (println attrs pattern index)
      ; Find leading attributes present in `pattern`.
      (if-let [value (get pattern attr)]
        ; Narrow scope by recursively getting next level of indexes.
        (recur more (get index value) (dissoc pattern attr))
        ; Filter remaining entries by remaining pattern attributes.
        (let [records
              (apply concat
                     (nth (iterate (partial mapcat vals)
                                   [index])
                          (count attrs)))
              matches?
              (fn [record]
                (every? #(= (get pattern %) (get record %))
                        (keys pattern)))]
          (filter matches? records)))))

  (update
    [{:keys [attrs index] :as this} record]
    (let [key-vec (vec (map record attrs))]
      (when (some nil? key-vec)
        (throw (IllegalArgumentException.
                 (str "Cannot update index with record missing required "
                      "attributes " (pr-str attrs) " " (pr-str record)))))
      (assoc this :index
             (update-sorted-in index
                               key-vec
                               (fnil conj (sorted-set-by order/rank)) record)))))


(defn memory-index
  [& attrs]
  {:pre [(seq attrs)]}
  (MemoryIndex. (vec attrs) (sorted-map)))
