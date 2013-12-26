(ns vault.blob.store.memory
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [vault.blob.core :as blob :refer [BlobStore]]))


(defrecord MemoryBlobStore
  [store]

  BlobStore

  (-list [this opts]
    (blob/select-ids opts (keys @store)))


  (-stat [this id]
    (when-let [blob (@store id)]
      {:size (count (byte-streams/to-byte-array (:content blob)))
       :stored-at (:since blob)}))


  (-open [this id]
    (when-let [blob (@store id)]
      (byte-streams/to-input-stream (:content blob))))


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
