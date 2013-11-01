(ns vault.store.memory
  (:require
    [clojure.java.io :as io]
    [vault.blob :refer :all]
    [vault.store :refer :all]))


;; IN-MEMORY STORE

(defrecord MemoryBlobStore
  [algorithm store]

  BlobStore

  (enumerate [this]
    (enumerate this {}))

  (enumerate [this opts]
    (select-blobrefs opts (keys @store)))


  (stat [this blobref]
    (when-let [blob (@store blobref)]
      {:size (count blob)}))


  (open [this blobref]
    (when-let [blob (@store blobref)]
      (io/input-stream blob)))


   (store! [this content]
     (with-open [buffer (java.io.ByteArrayOutputStream.)]
       (io/copy content buffer)
       (let [data (.toByteArray buffer)
             blobref (hash-content algorithm data)]
         (swap! store assoc blobref data)
         blobref))))


(defn memory-store
  "Creates a new in-memory blob store."
  [algorithm]
  (MemoryBlobStore. algorithm
                    (atom (sorted-map) :validator map?)))
