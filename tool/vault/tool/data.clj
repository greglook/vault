(ns vault.tool.data
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [mvxcvi.crypto.pgp :as pgp]
    [puget.printer :as puget]
    [vault.blob.store :as store]
    [vault.data.edn :as edn]
    [vault.data.key :as key]
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


(defn- print-key-blob
  "Prints a public-key blob info."
  [blob]
  (-> blob :data/values first pgp/key-info puget/cprint))


(defn- print-edn-blob
  "Prints an EDN blob's values."
  [blob]
  (puget/with-color
    (edn/print-data blob)))



;; DATA ACTIONS

(defn show-blob
  [opts args]
  (let [store (:blob-store opts)]
    (doseq [id (apply enumerate-prefix store args)]
      (when-let [blob (store/get store id)]
        (println (str id))
        (let [content (:content blob)
              key (key/parse-key blob)
              data (edn/parse-data blob)]
          (cond key                (print-key-blob key)
                data               (print-edn-blob data)
                (:binary opts)     (print-binary-blob content)
                (textual? content) (print-text-blob content)
                :else              (print-binary-blob content)))
        (newline)))))
