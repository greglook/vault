(ns vault.search.engine.brute
  (:require
    [vault.blob.core :as blob]))


;;;;; BRUTE-FORCE INDEX ;;;;;

(defrecord BruteForceEngine
  [store projection])

#_
(extend-type BruteForceEngine
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
