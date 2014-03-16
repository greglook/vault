(ns vault.data.format
  "Functions to handle structured data formatted as EDN."
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.edn :as edn]
    [clojure.string :as string]
    [puget.data]
    [puget.printer :as puget])
  (:import
    (java.io
      ByteArrayOutputStream
      FilterReader
      InputStream
      InputStreamReader
      OutputStreamWriter
      PushbackReader
      Reader
      Writer)
    java.nio.charset.Charset))


;; CONSTANTS & CONFIGURATION

(def ^Charset blob-charset
  (Charset/forName "UTF-8"))


(def ^:const blob-header
  "Magic header which must appear as the first characters in a data blob."
  "#vault/data\n")


(def ^:private ^:const blob-width
  "Width of text to use in serialized blobs."
  100)



;; SERIALIZATION

(defn- print-value
  "Prints the canonical EDN representation for the given Clojure value."
  [value]
  (let [print-opts {:width blob-width}]
    (puget/pprint value print-opts)))


(defn value-bytes
  "Computes an array of bytes representing the serialized form of the given
  value."
  [value]
  (binding [puget/*colored-output* false]
    (-> value
        print-value
        with-out-str
        string/trim
        (.getBytes blob-charset))))


(defn print-data
  "Prints the given data value(s) as canonical EDN in a data blob."
  [value & more]
  (binding [puget/*strict-mode* true]
    (print (puget/color-text :tag blob-header))
    (print-value value)
    (doseq [v more]
      (newline)
      (print-value v))))


(defn print-data-str
  "Prints a canonical EDN representation to a string and returns it. This
  function disables colorization."
  [value & more]
  (binding [puget/*colored-output* false]
    (string/trim (with-out-str (apply print-data value more)))))



;; DESERIALIZATION

(defn- capturing-reader
  "Wraps the given reader with a proxy which records the characters read to the
  given writer."
  [^Reader reader
   ^Writer writer]
  (proxy [FilterReader] [reader]
    (read []
      (let [c (.read reader)]
        (.write writer c)
        c))))


(defn- read-header!
  "Reads the first few bytes from a data source to determine whether it is a
  data blob. The result is true if the header matches, otherwise false."
  [source]
  (let [magic-bytes (.getBytes blob-header blob-charset)
        magic-len (count magic-bytes)
        header-bytes (byte-array magic-len)]
    (with-open [^InputStream input
                (byte-streams/to-input-stream source)]
      (.read input header-bytes 0 magic-len))
    (bytes= magic-bytes header-bytes)))


(defn- read-primary-value!
  "Reads the primary EDN value from a data blob. If *primary-bytes* is
  thread-bound, it will be set to an array of bytes which form the value. This
  is accomplished by copying the read characters into a byte array as they are
  consumed by the EDN parser. Returns a vector of the parsed value and a buffer
  of the bytes which form it."
  [tag-readers reader]
  (let [copy-bytes (ByteArrayOutputStream.)
        copy-writer (OutputStreamWriter. copy-bytes blob-charset)
        reader (PushbackReader. (capturing-reader reader copy-writer))
        value (edn/read {:readers tag-readers} reader)]
    (.flush copy-writer)
    (vector value (.toByteArray copy-bytes))))


(defn- read-secondary-values!
  "Reads the secondary EDN values from a data blob. Returns a seq of the values
  read."
  [tag-readers reader]
  (let [opts {:eof ::end-stream
              :readers tag-readers}
        reader (PushbackReader. reader)
        read-stream (partial edn/read opts reader)
        edn-stream (repeatedly read-stream)
        not-eos? (partial not= ::end-stream)]
    (doall (take-while not-eos? edn-stream))))


(defn read-data
  "Reads the given byte content and attempts to parse it as an EDN data
  structure. If the content is not EDN, it returns nil. Otherwise, it returns a
  sequence of the parsed values.

  The returned sequence will have attached metadata giving the bytes which
  comprise the first value in the sequence. This is accessible using the
  'primary-bytes' function."
  ([content]
   (read-data nil content))
  ([tag-readers content]
   (when (read-header! content)
     (let [tag-readers (merge puget.data/data-readers
                              tag-readers)]
       (with-open [reader (-> content
                              byte-streams/to-input-stream
                              (InputStreamReader. blob-charset))]
         (.skip reader (count blob-header))
         (let [[pvalue pbytes] (read-primary-value! tag-readers reader)
               svalues (read-secondary-values! tag-readers reader)]
           (with-meta
             (cons pvalue svalues)
             {::primary-bytes pbytes})))))))


(defn primary-bytes
  "Retrieves the array of bytes comprising the primary data value from the
  metadata on a value sequence."
  [data]
  (::primary-bytes (meta data)))
