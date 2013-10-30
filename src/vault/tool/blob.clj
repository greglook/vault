(ns vault.tool.blob
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [vault.store :as blobs]))


(defn list-blobs
  [opts args]
  (let [store (:store opts)
        controls (select-keys opts [:start :count])
        blobs (blobs/enumerate store controls)]
    (doseq [blobref blobs]
      (println (str blobref)))))


(defn blob-info
  [opts args]
  (let [store (:store opts)]
    (doseq [blobref (apply blobs/enumerate-prefixes store args)]
      (let [info (blobs/stat store blobref)]
        (if (:pretty opts)
          (do
            (println (str blobref))
            (pprint info)
            (newline))
          (do
            (print (str blobref) \space)
            (prn info)))))))


(defn get-blob
  [opts args]
  (when (or (empty? args) (> (count args) 1))
    (println "Must provide a single blobref or unique prefix.")
    (System/exit 1))
  (let [store (:store opts)
        blobrefs (blobs/enumerate-prefixes store (first args))]
    (when (> (count blobrefs) 1)
      (println (count blobrefs) "blobs match prefix:")
      (doseq [blobref blobrefs]
        (println (str blobref)))
      (System/exit 1))
    (with-open [stream (blobs/open store (first blobrefs))]
      (io/copy stream *out*))))


(defn put-blob
  [opts args]
  (let [blobref (blobs/store! (:store opts) *in*)]
    (println (str blobref))))
