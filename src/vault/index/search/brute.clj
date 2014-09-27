(ns vault.index.search.brute
  (:require
    [vault.blob.core :as blob]
    [vault.index.search :as search]))


;;;;; BRUTE-FORCE INDEX ;;;;;

(defrecord BruteForceEngine
  [store projection]

  search/SearchEngine

  (update!
    [this record]
    ; no-op
    this)

  (search*
    [this pattern opts]
    ; Exhaustively search projections of stored blobs.
    (filter (partial search/matches? pattern)
            (mapcat projection (blob/list store)))))


(defn brute-force-engine
  [blob-store projection]
  (BruteForceEngine. blob-store projection))
