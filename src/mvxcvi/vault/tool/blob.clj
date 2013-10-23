(ns mvxcvi.vault.tool.blob
  (:require [clojure.pprint :refer [pprint]]
            [mvxcvi.vault.blob.store :as store]))

(defn- get-blob-store
  [blob-stores nickname]
  (if (keyword? nickname)
    (let [target (blob-stores nickname)]
      (if (keyword? target)
        (recur blob-stores target)
        target))))


(defn list-blobs
  [opts args]
  (let [blob-stores (:blob-stores opts)
        store (get-blob-store blob-stores (:store opts :default))]
    (if-not store
      (throw (IllegalStateException. "No blob-store exists."))
      (doseq [blobref (store/enumerate store)]
        (println (str blobref))))))


(defn blob-info
  [opts args]
  (println "Getting blob info")
  (pprint [opts args]))


(defn get-blob
  [opts args]
  (println "Getting blob content")
  (pprint [opts args]))


(defn put-blob
  [opts args]
  (println "Storing blob content")
  (pprint [opts args]))
