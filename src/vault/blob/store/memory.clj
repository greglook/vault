(ns vault.blob.store.memory
  (:require
    [clojure.java.io :as io]
    (vault.blob
      [core :as blob :refer [BlobStore]]
      [digest :as digest]))
  (:import
    (java.io
      ByteArrayInputStream)))


(defrecord MemoryBlobStore
  [store]

  BlobStore

  (-list [this opts]
    (digest/select-ids opts (keys @store)))


  (-stat [this id]
    (when-let [blob (@store id)]
      {:size (count (:content blob))
       :stored-at (:since blob)}))


  (-open [this id]
    (when-let [blob (@store id)]
      (ByteArrayInputStream. (:content blob))))


  (-store! [this blob]
    (let [id (:id blob)]
      (when-not (@store id)
        (swap! store assoc id (assoc blob :since (java.util.Date.))))))


  (-remove! [this id]
    (when (@store id)
      (swap! store dissoc id)
      true)))


(defn memory-store
  "Creates a new in-memory blob store."
  []
  (MemoryBlobStore. (atom (sorted-map) :validator map?)))
