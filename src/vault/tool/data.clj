(ns vault.tool.data
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [puget.printer :as puget]
    [vault.blob.core :as blob]
    [vault.data.format :as data]
    [vault.tool.blob :refer [enumerate-prefix]]))


;; UTILITY FUNCTIONS

(defn- print-binary-blob
  "Prints a binary blob as a hex editor view."
  [content]
  (byte-streams/print-bytes content))


(defn- print-text-blob
  "Prints a text blob as plain text."
  [content]
  (with-open [input (byte-streams/to-input-stream content)]
    (io/copy *out*)))


(defn- print-data-blob
  "Prints a sequence of EDN data values."
  [data]
  (binding [puget/*colored-output* true]
    (apply data/print-data data)))



;; DATA ACTIONS

(defn show-blob
  [opts args]
  (let [store (:store opts)]
    (doseq [id (apply enumerate-prefix store args)]
      (when-let [blob (blob/get store id)]
        (println (str id))
        (let [data (data/read-data blob)]
          (if data
            (print-data-blob data)
            ; TODO: check for all ascii (or no null blobs?) first...
            (print-binary-blob (:content blob))))
        (newline)))))
