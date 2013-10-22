(ns mvxcvi.vault.tool.blob
  (:require [clojure.pprint :refer [pprint]]))


(defn list-blobs
  [opts args]
  (println "Listing blobs")
  (pprint [opts args]))


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
