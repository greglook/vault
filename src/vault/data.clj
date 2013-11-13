(ns vault.data
  "Code to handle structured data, usually represented as EDN."
  (:require
    [clojure.string :as string]
    [fipp.printer :refer [pprint-document]]
    [puget.printer :as puget])
  (:import
    java.nio.charset.Charset))


(def ^:private ^:const blob-charset
  (Charset/forName "UTF-8"))


(def ^:private ^:const blob-width
  "Width of text to use in serialized blobs."
  100)


(def ^:private ^:const blob-header
  "Magic header which must appear as the first characters in a data blob."
  "#vault/data")


(defn- serialize-value
  "Returns the canonical EDN representation for the given Clojure value."
  [value]
  (binding [puget/*colored-output* false
            puget/*strict-mode* true]
    (-> value
        (puget/pprint {:width blob-width})
        with-out-str
        string/trim)))


(defn edn-blob
  "Returns a canonical EDN representation suitable for serializing to a blob."
  [value]
  (let [edn (serialize-value value)
        metadata (select-keys (meta value) [:type :vault/version])
        lines (if (empty? metadata)
                [blob-header edn]
                [blob-header (str \^ (serialize-value metadata)) edn])]
    (string/join \newline lines)))


(def data-readers
  {'bin puget/read-bin
   'uri puget/read-uri})


(defn read-data
  "Reads the given input stream and attempts to parse it as an EDN data
  structure. If the data is not EDN, it returns a byte array of the blob
  contents."
  [input]
  ; input > capturing proxy > InputStreamReader > PushbackReader
  ; input > PushbackInputStream > capturing proxy > InputStreamReader > PushbackReader
  ; - read first (count blob-header) bytes from the stream
  ; - if no match, unread the bytes before returning the PushbackInputStream
  ; - otherwise, read remainder of stream and parse out EDN value(s)
  ; - first value is primary value - then save the output of the capturing proxy
  ;     - ideally, disable it
  ; - read remaining values (just signatures?)
  ;     - attach as value metadata?
  nil)
