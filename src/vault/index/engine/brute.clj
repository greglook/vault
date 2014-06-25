(ns vault.index.engine.brute
  (:require
    [vault.blob.core :as blob]
    [vault.index.engine :as engine]))


;;;;; BRUTE-FORCE INDEX ;;;;;

(defrecord BruteForceIndex
  [blob-store projection])

(extend-type BruteForceIndex
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
            (mapcat (:projection this)
                    (blob/list (:blob-store this))))))


(defn brute-force-index
  [blob-store projection]
  (BruteForceIndex. blob-store projection))
