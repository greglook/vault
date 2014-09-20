(ns vault.blob.store.memory
  (:require
    [clojure.java.io :as io]
    [clj-time.core :as time]
    [vault.blob.store :as store]))


;;;;; HELPER FUNCTIONS ;;;;;

(defn- get-mem
  "Gets a blob out of a memory blob store by id."
  [store id]
  (-> store :memory deref (get id)))


(defn- blob-stats
  "Augments a blob with stat metadata."
  [blob]
  (assoc blob
    :stat/size (count (:content blob))
    :stat/stored-at (or (:stat/stored-at blob)
                        (time/now))))



;;;;; MEMORY STORE ;;;;;

(defrecord MemoryBlobStore
  [memory])

(extend-type MemoryBlobStore
  store/BlobStore

  (enumerate [this opts]
    (store/select-ids opts (-> this :memory deref keys)))


  (stat [this id]
    (when-let [blob (get-mem this id)]
      (dissoc blob :content)))


  (get [this id]
    (get-mem this id))


  (put! [this blob]
    (if-let [id (:id blob)]
      (or (get-mem this id)
          (let [blob (blob-stats blob)]
            (swap! (:memory this) assoc id blob)
            blob))))


  (delete! [this id]
    (swap! (:memory this) dissoc id)))


(defn memory-store
  "Creates a new in-memory blob store."
  []
  (MemoryBlobStore. (atom (sorted-map) :validator map?)))
