(ns vault.search.index.brute
  "Brute-force indexes scan stored blobs to find records."
  (:require
    [vault.blob.store :as store]
    [vault.search.index :as index]))


;; A brute-force index does not actually store records. Instead, it uses a
;; backing blob store (containing the blobs it is 'indexing') and a projection
;; function which can convert blobs to records.
;;
;; When queried, the stored blobs are scanned for records matching the given
;; clauses. Inserting records is a no-op, and attempting to delete records will
;; cause an error.
(defrecord BruteForceIndex
  [store projection]

  index/Index

  #_
  (search*
    [this query]
    (->> (store/list store)
         (mapcat (comp projection store/get))
         (index/filter-records query)))


  (insert!
    [this record]
    this))


(defn brute-force-index
  "Constructs a new brute-force 'index' which will search records from the
  given blob store."
  [opts]
  {:pre [(some? (:store opts))
         (fn? (:projection opts))]}
  (map->BruteForceIndex opts))
