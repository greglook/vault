(ns vault.index.search.brute
  (:require
    [vault.blob.core :as blob]
    [vault.index.search :as search]))


;;;;; BRUTE-FORCE INDEX ;;;;;

(defrecord BruteForceIndex
  [blob-store projection])

(extend-type BruteForceIndex
  search/SearchEngine

  (init!
    [this]
    ; no-op
    this)

  (update!
    [this record]
    ; no-op
    this)

  (search*
    [this pattern opts]
    ; Exhaustively search projections of stored blobs.
    (filter (partial search/matches? pattern)
            (mapcat (:projection this)
                    (blob/list (:blob-store this))))))


(defn brute-force-index
  [blob-store projection]
  (BruteForceIndex. blob-store projection))
