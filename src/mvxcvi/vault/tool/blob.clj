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
      (let [info (blobs/stat store blobref)]
        (if (:pretty opts)
          (do
            (println (str blobref))
            (pprint info)
            (println))
          (println (str blobref) info))))))


(defn get-blob
  [opts args]
  (when (empty? args)
    (println "First argument must be a blobref or unique prefix.")
    (System/exit 1))
  (let [store (:store opts)
        blobrefs (blobs/find-prefix store (first args))]
    (when (> (count blobrefs) 1)
      (println "Multiple blobs match prefix: " blobrefs)
      (System/exit 1))
    (with-open [stream (blobs/open store (first blobrefs))]
      (io/copy stream *out*))))


(defn put-blob
  [opts args]
  (let [blobref (blobs/store! (:store opts) *in*)]
    (println (str blobref))))
