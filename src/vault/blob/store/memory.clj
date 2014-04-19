(ns vault.blob.store.memory
  (:require
    [clojure.java.io :as io]
    [vault.blob.core :as blob :refer [BlobStore]]))


(defn- stat-blob
  "Augments a blob with stat metadata."
  [blob]
  (assoc blob
    :meta/size (count (:content blob))
    :meta/stored-at (or (:meta/stored-at blob)
                        (java.util.Date.))))


(defrecord MemoryBlobStore
  [store]

  BlobStore

  (enumerate [this opts]
    (blob/select-ids opts (keys @store)))


  (stat [this id]
    (when-let [blob (@store id)]
      (dissoc blob :content)))


  (get* [this id]
    (@store id))


  (put! [this blob]
    (if-let [id (:id blob)]
      (or (@store id)
          (let [blob (stat-blob blob)]
            (swap! store assoc id blob)
            blob)))))


(defn delete!
  [this id]
  (when (@(:store this) id)
    (swap! (:store this) dissoc id)
    true))


(defn destroy!!
  [this]
  (swap! (:store this) empty))


(defn memory-store
  "Creates a new in-memory blob store."
  []
  (MemoryBlobStore. (atom (sorted-map) :validator map?)))
