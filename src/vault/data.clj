(ns vault.data
  "Functions to handle structured data represented as EDN."
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]
    [fipp.printer :refer [pprint-document]]
    [puget.printer :as puget])
  (:import
    java.io.ByteArrayOutputStream
    java.io.FilterReader
    java.io.InputStreamReader
    java.io.OutputStreamWriter
    java.io.PushbackReader
    java.nio.charset.Charset))


;; CONSTANTS & CONFIGURATION

(def ^:private blob-charset
  (Charset/forName "UTF-8"))


(def ^:private ^:const blob-width
  "Width of text to use in serialized blobs."
  100)


(def ^:private ^:const blob-header
  "Magic header which must appear as the first characters in a data blob."
  "#vault/data\n")



;; SERIALIZATION

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



;; DESERIALIZATION

(defn- read-header!
  "Reads the first few bytes from an input stream to determine whether it is a
  data blob. The result is true if the header matches, and the stream is left
  positioned after the header bytes. Otherwise, it is reset back to the start of
  the stream."
  [input]
  (let [magic-bytes (.getBytes blob-header blob-charset)
        magic-len (count magic-bytes)
        header-bytes (byte-array magic-len)]
    (.mark input magic-len)
    (.read input header-bytes 0 magic-len)
    (if (= (seq magic-bytes) (seq header-bytes))
      true
      (do
        (.reset input)
        false))))


(def ^:dynamic *primary-bytes*
  "If bound when `read-data` is called, this var is set to an array containing
  the bytes which comprise the 'primary' EDN value in a data blob. These bytes
  are the target for inline signatures."
  nil)


(defn- capturing-reader
  "Wraps the given reader with a proxy which records the characters read to the
  given output stream."
  [input output]
  (proxy [FilterReader] [input]
    (read
      ([]
       (let [c (.read input)]
         (.write output c)
         c)))))


(defn- read-primary-value!
  "Reads the primary EDN value from a data blob. If *primary-bytes* is
  thread-bound, it will be set to an array of bytes which form the value. This
  is accomplished by copying the read characters into a byte array as they are
  consumed by the EDN parser. Returns the parsed value."
  [reader]
  (let [copy-bytes (ByteArrayOutputStream.)
        copy-writer (OutputStreamWriter. copy-bytes blob-charset)
        reader (PushbackReader. (capturing-reader reader copy-writer))
        ; TODO: specify readers? {:readers puget.data/data-readers}
        value (edn/read reader)]
    (when (thread-bound? #'*primary-bytes*)
      (.flush copy-writer)
      (set! *primary-bytes* (.toByteArray copy-bytes)))
    value))


(defn read-data
  "Reads the given input stream and attempts to parse it as an EDN data
  structure. If the data is not EDN, it returns an input stream of the blob
  contents."
  [input]
  ; TODO: ensure `mark` is supported
  (if (read-header! input)
    (let [reader (InputStreamReader. input blob-charset)
          primary-value (read-primary-value! reader)]
      ; TODO: read remaining values from the stream
      primary-value)
    input))
