(ns vault.blob.store.memory
  (:require
    [clojure.java.io :as io]
    [vault.blob.store :as store :refer [BlobStore]])
  (:import
    (java.io
      ByteArrayInputStream
      ByteArrayOutputStream)))


(defn- blob-status
  [blob]
  (let [{:keys [status data]} blob]
    (merge status {:size (count data)})))


(defrecord MemoryBlobStore
  [store]

  BlobStore

  (-list [this opts]
    (store/select-refs opts (keys @store)))


  (-stat [this blobref]
    (when-let [blob (@store blobref)]
      (blob-status blob)))


  (-open [this blobref]
    (when-let [blob (@store blobref)]
      [(blob-status blob)
       (ByteArrayInputStream. (:data blob))]))


  (-store! [this blobref stream status]
    (with-open [buffer (ByteArrayOutputStream.)]
      (io/copy stream buffer)
      (let [data (.toByteArray buffer)
            blob {:data data, :status status}]
        (swap! store assoc blobref blob)
        blobref)))


  (-remove! [this blobref]
    (when (contains? @store blobref)
      (swap! store dissoc blobref)
      true)))


(defn memory-store
  "Creates a new in-memory blob store."
  []
  (MemoryBlobStore. (atom (sorted-map) :validator map?)))
