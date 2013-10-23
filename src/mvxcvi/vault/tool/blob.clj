(ns mvxcvi.vault.tool.blob
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [mvxcvi.vault.blob.store :as store]))

(defn list-blobs
  [opts args]
  (let [blobs (store/enumerate (:store opts)
                               (select-keys opts [:start :count]))]
    (doseq [blobref blobs]
      (println (str blobref)))))


(defn blob-info
  [opts args]
  (println "Getting blob info")
  (pprint [opts args]))


(defn get-blob
  [opts args]
  (when (empty? args)
    (println "First argument must be a blobref or unique prefix.")
    (System/exit 1))
  (let [prefix (first args)
        store (:store opts)
        blobs (store/enumerate store {:start prefix :count 5})]
    (when (< 1 (count blobs))
      (println "Multiple blobs match prefix: " blobs)
      (System/exit 1))
    (with-open [stream (store/content-stream (:store opts) (first blobs))]
      (io/copy stream *out*))))


(defn put-blob
  [opts args]
  (println "Storing blob content")
  (pprint [opts args]))
