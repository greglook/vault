(ns vault.store.memory
  (:require
    [clojure.java.io :as io]
    [vault.blob :as blob :refer [BlobStore]]))


;; IN-MEMORY STORE

(defrecord MemoryBlobStore
  [algorithm store]

  BlobStore

  (-algorithm [this]
    algorithm)


  (-list [this opts]
    (blob/select-refs opts (keys @store)))


  (-stat [this blobref]
    (when-let [blob (@store blobref)]
      {:size (count blob)}))


  (-open [this blobref]
    (when-let [blob (@store blobref)]
      (io/input-stream blob)))


  (-store! [this content]
    (with-open [buffer (java.io.ByteArrayOutputStream.)]
      (io/copy content buffer)
      (let [data (.toByteArray buffer)
            blobref (blob/digest algorithm data)]
        (swap! store assoc blobref data)
        blobref)))


  (-remove! [this blobref]
    (when (contains? @store blobref)
      (swap! store dissoc blobref)
      true)))


(defn memory-store
  "Creates a new in-memory blob store."
  [algorithm]
  (MemoryBlobStore. algorithm
                    (atom (sorted-map) :validator map?)))
