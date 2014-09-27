(ns vault.search.catalog
  (:require
    [clj-time.core :as time]
    [vault.blob.store :as store]
    [vault.search.index :as index]))


;;;;; HELPER FUNCTIONS ;;;;;

(defn- get-blob-index
  [catalog]
  (get (:indexes catalog) (:blob-key catalog)))



;;;;; INDEX CATALOG ;;;;;

(defrecord IndexCatalog
  [indexes blob-key]

  store/BlobStore

  (enumerate
    [this opts]
    (store/select-ids opts
      ; TODO: this is where being able to do real queries would help;
      ; specifically, for :after and :prefix.
      (index/seek (get-blob-index this))))


  (stat
    [this id]
    (when id
      ; TODO: rename :size to :stat/size, etc.
      (index/get (get-blob-index this) {:blob id})))


  (put!
    [this blob]
    (when-not (store/stat this (:id blob))
      ; TODO: ensure blob has been parsed?
      (doseq [index (vals (:indexes this))]
        (when-let [projection (:projection index)]
          (doseq [record (projection blob)]
            (index/insert! index record)))))
    blob))


(defn catalog
  "Creates a new catalog out of the given indexes. The keyword given names
  the index to look up blob stats from."
  [indexes blob-key]
  {:pre [(contains? indexes blob-key)]}
  (IndexCatalog. indexes blob-key))
