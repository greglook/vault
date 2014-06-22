(ns vault.index.engine.brute
  (:require
    [vault.blob.core :as blob]
    [vault.index.engine :as engine]))


(defrecord BruteSearchEngine
  [blob-store projection])

(extend-type BruteSearchEngine
  engine/SearchEngine

  (init!
    [this]
    ; no-op
    this)


  (update!
    [this record]
    ; no-op
    this)


  (search
    [this pattern opts]
    ; exhaustively search projections of stored blobs
    (filter (partial engine/matches? pattern)
            (mapcat projection
                    (blob/list (:blob-store this))))))


(defn brute-index
  [blob-store projection]
  (BruteSearchEngine. blob-store projection))
