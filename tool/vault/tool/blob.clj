(ns vault.tool.blob
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [puget.printer :refer [cprint]]
    (vault.blob
      [core :as blob]
      [digest :as digest]
      [store :as store])))


;; HELPER FUNCTIONS

(defn- prefix-id
  "Adds the given algorithm to a hash-id if none is specified."
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
   (blob/list store :prefix (prefix-id digest/*algorithm* prefix)))
  ([store prefix & more]
   (mapcat (partial enumerate-prefix store) (cons prefix more))))



;; BLOB ACTIONS

(defn list-blobs
  [opts args]
  (let [store (:store opts)
        controls (select-keys opts [:after :prefix :limit])
        blobs (blob/list store controls)]
    (doseq [hash-id blobs]
      (println (str hash-id)))))


(defn blob-info
  [opts args]
  (let [store (:store opts)]
    (doseq [hash-id (apply enumerate-prefix store args)]
      (let [info (blob/stat store hash-id)]
        (if (:pretty opts)
          (do
            (println (str hash-id))
            (cprint info)
            (newline))
          (do
            (print (str hash-id) \space)
            (prn info)))))))


(defn get-blob
  [opts args]
  (when (or (empty? args) (> (count args) 1))
    (throw (IllegalArgumentException. "Must provide a single blobref or unique prefix.")))
  (let [store (:store opts)
        ids (enumerate-prefix store (first args))]
    (when (< 1 (count ids))
      (throw (IllegalArgumentException.
               (str (count ids) " blobs match prefix: "
                    (str/join ids " ")))))
    (let [blob (blob/get store (first ids))]
      (io/copy (:content blob) *out*))))


(defn put-blob
  [opts args]
  (when (or (empty? args) (< 1 (count args)))
    (throw (IllegalArgumentException. "Must provide a single source of blob data.")))
  (let [source (io/file (first args))]
    (if-let [blob (blob/store! (:store opts) source)]
      (println (str (:id blob)))
      (binding [*out* *err*]
        (println "(no content)")))))
