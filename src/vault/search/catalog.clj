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


(defn- stats->blob
  "Constructs an empty blob with stat metadata from the blobs index."
  [record]
  (assoc
    (content/empty-blob (:blob record))
    :stat/size (:size record)
    :stat/stored-at (:stored-at record)
    :data/type (:type record)))


(defrecord IndexCatalog
  [blobs links tx-log datoms]

  store/BlobStore

  (enumerate
    [this opts]
    (->> (index/seek blobs {:blob (:after opts)})
         (map :blob)
         (store/select-ids opts)))


  (stat
    [this id]
    (when id
      (some->
        blobs
        (index/seek {:blob id})
        first
        stats->blob)))


  (put!
    [this blob]
    (when-not (store/stat this (:id blob))
      ; TODO: ensure blob has been parsed?
      (doseq [index (set (vals this))]
        (index/put! index blob)))
    blob))


(defn find-blobs
  "Look up blob stat records by type keyword and label."
  [catalog data-type label]
  (some->
    catalog :blobs
    (index/find :type data-type label)
    (->> (map stats->blob))))


(defn links-from
  "Look up data links by source hash-id."
  [catalog source-id]
  (some->
    catalog :links
    (index/find :source source-id)))


(defn links-to
  "Look up data sorted by target hash-id and source blob type."
  [catalog target-id data-type]
  (some->
    catalog :links
    (index/find :target target-id data-type)))


(defn catalog
  "Creates a new catalog out of the given index key/value pairs."
  [& {:as indexes}]
  {:pre [(contains? indexes :blobs)]}
  (map->IndexCatalog indexes))
