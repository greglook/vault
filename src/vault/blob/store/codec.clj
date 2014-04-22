(ns vault.blob.store.codec
  (:require
    [vault.blob.core :as blob :refer [BlobStore]]))


(defrecord CodecBlobStore
  [store encoders decoders]

  BlobStore

  (enumerate [this opts]
    (blob/enumerate store opts))


  (stat [this id]
    (blob/stat store id))


  (get* [this id]
    (when-let [blob (blob/get* store id)]
      (reduce #(or (%2 %1) %1) blob decoders)))


  (put! [this blob]
    (when-not (empty? (:content blob))
      (->>
        encoders
        (reduce #(or (%2 %1) %1) blob)
        (blob/put! store)))))


(defn codec-store
  "Wraps a blob store with encoders and decoders."
  [store encoders decoders]
  {:pre [(satisfies? BlobStore store)]}
  (let [vectorize #(cond
                     (nil? %) []
                     (vector? %) %
                     :else (vector %))]
    (CodecBlobStore.
      store
      (vectorize encoders)
      (vectorize decoders))))
