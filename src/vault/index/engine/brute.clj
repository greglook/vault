(ns vault.index.engine.brute
  (:require
    [vault.blob.core :as blob]
    [vault.index.search :as search]))


(defrecord BruteIndex [blob-store projection])

(extend-type BruteIndex
  search/Engine

  (search
    [this pattern opts]
    (filter (partial search/matches? pattern)
            (mapcat projection
                    (blob/list (:blob-store this)))))

  (update!
    [this record]
    ; It's not clear that this maps well to a brute-force search engine.
    ; TODO: figure out better semantics here.
    nil))


(defn brute-index
  [blob-store projection]
  (BruteIndex. blob-store projection))
