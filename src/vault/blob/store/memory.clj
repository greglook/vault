(ns vault.blob.store.memory
  "Blob storage backed by a map in memory."
  (:require
    [clj-time.core :as time]
    [clojure.java.io :as io]
    [vault.blob.store :as store]))


(defn- blob-stats
  "Augments a blob with stat metadata."
  [blob]
  (assoc blob
    :stat/size (count (:content blob))
    :stat/stored-at (or (:stat/stored-at blob)
                        (time/now))))



;; ## Memory Store

;; Blob records in a memory store are held in a map in an atom.

(defrecord MemoryBlobStore
  [memory]

  store/BlobStore

  (enumerate
    [this opts]
    (store/select-ids opts (keys @memory)))


  (stat
    [this id]
    (when-let [blob (get @memory id)]
      (dissoc blob :content)))


  (get*
    [this id]
    (get @memory id))


  (put!
    [this blob]
    (if-let [id (:id blob)]
      (or (get @memory id)
          (let [blob (blob-stats blob)]
            (swap! memory assoc id blob)
            blob))))


  (delete!
    [this id]
    (swap! memory dissoc id))


  (erase!!
    [this]
    (swap! memory empty)))


(defn memory-store
  "Creates a new in-memory blob store."
  []
  (MemoryBlobStore. (atom (sorted-map) :validator map?)))
