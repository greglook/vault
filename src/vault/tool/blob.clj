(ns vault.tool.blob
  (:require
    [clojure.java.io :as io]
    [puget.printer :refer [cprint]]
    [vault.blob.core :as blob]))


;; HELPER FUNCTIONS

(defn- prefix-id
  "Adds the given algorithm to a blobref if none is specified."
  [algorithm id]
  (if-not (some (partial = \:) id)
    (str (name algorithm) \: id)
    id))


(defn enumerate-prefix
  "Lists stored blobs with references matching the given prefixes.
  Automatically prepends the store's algorithm if none is given."
  ([store]
   (blob/list store))
  ([store prefix]
   (blob/list store :prefix (prefix-id blob/*digest-algorithm* prefix)))
  ([store prefix & more]
   (mapcat (partial enumerate-prefix store) (cons prefix more))))



;; BLOB ACTIONS

(defn list-blobs
  [opts args]
  (let [store (:store opts)
        controls (select-keys opts [:after :prefix :limit])
        blobs (blob/list store controls)]
    (doseq [blobref blobs]
      (println (str blobref)))))


(defn blob-info
  [opts args]
  (let [store (:store opts)]
    (doseq [blobref (apply enumerate-prefix store args)]
      (let [info (blob/stat store blobref)]
        (if (:pretty opts)
          (do
            (println (str blobref))
            (cprint info)
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
        ids (enumerate-prefix store (first args))]
    (when (< 1 (count ids))
      (println (count ids) "blobs match prefix:")
      (doseq [blobref ids]
        (println (str blobref)))
      (System/exit 1))
    (let [blob (blob/get store (first ids))]
      (io/copy (:content blob) *out*))))


(defn put-blob
  [opts args]
  (let [id (blob/put! (:store opts) *in*)]
    (println (str id))))
