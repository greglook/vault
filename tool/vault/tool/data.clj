(ns vault.tool.data
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [puget.printer :as puget]
    [vault.blob.core :as blob]
    [vault.data.edn :as edn-data]
    [vault.tool.blob :refer [enumerate-prefix]]))


;; UTILITY FUNCTIONS

(defn- textual?
  "Tries to determine if the given byte content is text."
  [content]
  (let [byte-seq (seq (byte-streams/to-byte-array content))]
    (every? (partial not= 0) byte-seq)))


(defn- print-binary-blob
  "Prints a binary blob as a hex editor view."
  [content]
  (byte-streams/print-bytes content))


(defn- print-text-blob
  "Prints a text blob as plain text."
  [content]
  (let [buffer (byte-streams/to-byte-array content)
        last-byte (last (seq buffer))]
    (io/copy buffer *out*)
    ; Add an extra newline if the text does not end with one already.
    (when (not= 10 last-byte)
      (println (str \u001b "[7m" \% \u001b "[0m")))))


(defn- print-edn-blob
  "Prints an EDN blob's values."
  [blob]
  (binding [puget/*colored-output* true]
    (edn-data/print-blob blob)))



;; DATA ACTIONS

(defn show-blob
  [opts args]
  (let [store (:store opts)]
    (doseq [id (apply enumerate-prefix store args)]
      (when-let [blob (blob/get store id)]
        (println (str id))
        (let [content (:content blob)
              data (edn-data/read-blob blob)]
          (cond data               (print-edn-blob data)
                (:binary opts)     (print-binary-blob content)
                (textual? content) (print-text-blob content)
                :else              (print-binary-blob content)))
        (newline)))))
