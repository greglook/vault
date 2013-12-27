(ns vault.blob.store.memory
  (:require
    [clojure.java.io :as io]
    [vault.blob.core :as blob :refer [BlobStore]]))


(defrecord MemoryBlobStore
  [store]

  BlobStore

  (enumerate [this opts]
    (blob/select-ids opts (keys @store)))


  (stat [this id]
    (when-let [blob (@store id)]
      {:size (count (:content blob))
       :stored-at (:stored-at blob)}))


  (open [this id]
    (when-let [blob (@store id)]
      (io/input-stream (:content blob))))


  (store! [this blob]
    (let [id (:id blob)]
      (when-not (@store id)
        (swap! store assoc id (assoc blob :stored-at (java.util.Date.))))))


  (delete! [this id]
    (when (@store id)
      (swap! store dissoc id)
      true))


  (destroy!! [this]
    (swap! store empty)))


(defn memory-store
  "Creates a new in-memory blob store."
  []
  (MemoryBlobStore. (atom (sorted-map) :validator map?)))
