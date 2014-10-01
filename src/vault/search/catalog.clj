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


(defn- blob-stats
  "Constructs an empty blob with stat metadata from the blobs index."
  [record]
  (assoc
    (content/empty-blob (:blob record))
    :stat/size (:size record)
    :stat/stored-at (:stored-at record)
    :data/type (:type record)))


(defrecord IndexCatalog
  [blobs refs txns datoms]

  store/BlobStore

  (enumerate
    [this opts]
    ; TODO: figure out query syntax for selecting records with ids :after
    (->> (index/search blobs :order :blob)
         (map :blob)
         (store/select-ids opts)))


  (stat
    [this id]
    (when id
      (when-let [record (first (index/search blobs :where {:blob id}))]
        (blob-stats record))))


  (put!
    [this blob]
    (when-not (store/stat this (:id blob))
      ; TODO: ensure blob has been parsed?
      (doseq [index (vals this)]
        (index/put! index blob)))
    blob))


(defn catalog
  "Creates a new catalog out of the given index key/value pairs."
  [& {:as indexes}]
  {:pre [(contains? indexes :blobs)]}
  (map->IndexCatalog indexes))
