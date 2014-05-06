(ns vault.blob.store.memory
  (:require
    [clojure.java.io :as io]
    [vault.blob.store :as store :refer [BlobStore]]))


(defn- blob-stats
  "Augments a blob with stat metadata."
  [blob]
  (assoc blob
    :stat/size (count (:content blob))
    :stat/stored-at (or (:stat/stored-at blob)
                        (java.util.Date.))))


(defrecord MemoryBlobStore
  [memory]

  BlobStore

  (enumerate [this opts]
    (store/select-ids opts (keys @memory)))


  (stat [this id]
    (when-let [blob (@memory id)]
      (dissoc blob :content)))


  (get* [this id]
    (@memory id))


  (put! [this blob]
    (if-let [id (:id blob)]
      (or (@memory id)
          (let [blob (blob-stats blob)]
            (swap! memory assoc id blob)
            blob)))))


(defn delete!
  [store id]
  (when (@(:memory store) id)
    (swap! (:memory store) dissoc id)
    true))


(defn destroy!!
  [store]
  (swap! (:memory store) empty))


(defn memory-store
  "Creates a new in-memory blob store."
  []
  (MemoryBlobStore. (atom (sorted-map) :validator map?)))
