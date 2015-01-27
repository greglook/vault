(ns vault.search.catalog
  "A catalog contains many indexes and wraps a write-only `BlobStore` interface
  around them for updates. Each index must define a `:projection` function which
  converts blobs to a sequence of records to store."
  (:require
    [clj-time.core :as time]
    (vault.blob
      [content :as content]
      [store :as store])
    [vault.search.index :as index]))




(defrecord IndexCatalog
  [blobs links tx-log datoms]

  store/BlobStore

  (enumerate
    [this opts]
    (->> (index/seek blobs {:id (:after opts)})
         (map :id)
         (store/select-ids opts)))


  (stat
    [this id]
    (when id
      (some->
        blobs
        (index/seek {:blob id})
        first)))


  (put!
    [this blob]
    (when-not (store/stat this (:id blob))
      ; TODO: ensure blob has been parsed?
      (doseq [index (set (vals this))]
        (index/put! index blob)))
    blob))


(defn catalog
  "Creates a new catalog out of the given index key/value pairs."
  [& {:as indexes}]
  {:pre [(contains? indexes :blobs)]}
  (map->IndexCatalog indexes))
