(ns mvxcvi.vault.tool.blob
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [mvxcvi.vault.blob.store :as blobs]))


(defn list-blobs
  [opts args]
  (let [store (:store opts)
        controls (select-keys opts [:start :count])
        blobs (blobs/enumerate store controls)]
    (doseq [blobref blobs]
      (println (str blobref)))))


(defn blob-info
  [opts args]
  (let [store (:store opts)
        blobrefs (if (empty? args)
                   (blobs/enumerate store)
                   (for [prefix args
                         blobref (blobs/find-prefix store prefix)]
                     blobref))]
    (doseq [blobref blobrefs]
      (println (str blobref) (blobs/blob-info store blobref)))))


(defn get-blob
  [opts args]
  (when (empty? args)
    (println "First argument must be a blobref or unique prefix.")
    (System/exit 1))
  (let [store (:store opts)
        blobs (blobs/find-prefix store (first args))]
    (when (> (count blobs) 1)
      (println "Multiple blobs match prefix: " blobs)
      (System/exit 1))
    (with-open [stream (blobs/content-stream store (first blobs))]
      (io/copy stream *out*))))


(defn put-blob
  [opts args]
  (let [blobref (blobs/store-content! (:store opts) *in*)]
    (println (str blobref))))
