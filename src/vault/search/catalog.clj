(ns vault.search.catalog
  (:require
    [clj-time.core :as time]
    (vault.blob
      [content :as content]
      [store :as store])
    [vault.search.index :as index]))


(defrecord IndexCatalog
  [indexes blob-key]

  store/BlobStore

  (enumerate
    [this opts]
    ; TODO: figure out query syntax for selecting records with ids :after
    (let [blob-index (get indexes blob-key)]
      (->> {:order :id}
           (index/search blob-index)
           (map :id)
           (store/select-ids opts))))


  (stat
    [this id]
    (when-let [{:keys [size stored-at type]}
               (and id (-> (get indexes blob-key)
                           (index/search :where {:id id})
                           first))]
      (assoc
        (content/empty-blob id)
        :stat/size size
        :stat/stored-at stored-at
        :data/type type)))


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
