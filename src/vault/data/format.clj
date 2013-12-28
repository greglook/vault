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
    java.nio.charset.Charset
    java.nio.ByteBuffer))


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
  (let [print-opts {:width blob-width}
        metadata (meta value)]
    (when-not (empty? metadata)
      (print (puget/color-text :delimiter \^))
      (puget/pprint metadata print-opts))
    (puget/pprint value print-opts)))


(defn print-data
  "Prints the given data value(s) as canonical EDN in a data blob."
  [value & more]
  (binding [puget/*strict-mode* true]
    (print (puget/color-text :tag blob-header))
    (print-value value)
    (dorun (map #(do (print "\n") (print-value %)) more))))


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
    (vector value (-> copy-bytes
                      .toByteArray
                      ByteBuffer/wrap
                      .asReadOnlyBuffer))))


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


(defn- read-values!
  "Reads primary and secondary EDN values from blob contents. Returns a seq of
  the values. The sequence will have attached metadata giving the bytes which
  comprise the first value in the sequence."
  [tag-readers content]
  (let [tag-readers (merge puget.data/data-readers tag-readers)]
    (with-open [reader (-> content
                           byte-streams/to-input-stream
                           (InputStreamReader. blob-charset))]
      (.skip reader (count blob-header))
      (let [[primary-value primary-bytes] (read-primary-value! tag-readers reader)
            secondary-values (read-secondary-values! tag-readers reader)]
        (-> primary-value
            (cons secondary-values)
            (vary-meta assoc ::primary-bytes primary-bytes))))))


(defn read-data
  "Reads the given blob data and attempts to parse it as an EDN data
  structure. If the data is not EDN, it returns nil. Otherwise, it returns a
  sequence of the parsed values.

  The returned sequence will have attached metadata giving the bytes which
  comprise the first value in the sequence."
  ([blob]
   (read-data nil blob))
  ([tag-readers blob]
   (let [{:keys [id content]} blob]
     (when (read-header! content)
       (let [data (read-values! tag-readers content)]
         (vary-meta data assoc ::blob-id id))))))


(defn primary-bytes
  "Retrieves the array of bytes comprising the primary data value from the
  metadata on a value sequence."
  [data]
  (::primary-bytes (meta data)))


(defn blob-id
  "Retrieves the identity of the blob which a sequence of data was read from."
  [data]
  (::blob-id (meta data)))
