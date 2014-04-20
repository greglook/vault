(ns vault.data.format.edn
  "Functions to read structured data formatted as EDN."
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.string :as str]
    [puget.data :as edn]
    [puget.printer :as puget])
  (:import
    (java.io
      ByteArrayOutputStream
      FilterInputStream
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
        str/trim
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
    (str/trim (with-out-str (apply print-data value more)))))



;; DESERIALIZATION

(defn- counting-input-stream
  "Wraps the given input stream with a proxy which counts the bytes read
  through it. The first argument should be an atom containing a counter."
  [^InputStream input
   counter]
  (proxy [FilterInputStream] [input]
    (read []
      (let [b (.read input)]
        (swap! counter inc)
        b))))


(defn- read-header!
  "Reads the first few bytes from a blob's content to determine whether it is a
  data blob. The result is true if the header matches, otherwise false."
  [^bytes content]
  (let [magic-bytes (.getBytes blob-header blob-charset)
        magic-len (count magic-bytes)]
    (= (seq magic-bytes) (take magic-len (seq content)))))


(defn- read-primary-value!
  "Reads the primary EDN value from a data blob. Returns a vector containing
  the primary value and the range of bytes as a second vector."
  [tag-readers reader bytes-read]
  (let [byte-start @bytes-read
        value (edn/read {:readers tag-readers} reader)
        byte-range [byte-start @bytes-read]]
    [value byte-range]))


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


(defn read-edn-blob
  "Reads the contents of the given blob and attempts to parse it as an EDN data
  structure. Returns an updated blob record, or nil if the content is not EDN."
  [blob]
  (when (read-header! (:content blob))
    (let [bytes-read (atom 0)]
      (with-open [reader (-> (:content blob)
                             byte-streams/to-input-stream
                             (counting-input-stream bytes-read)
                             (InputStreamReader. blob-charset)
                             PushbackReader.)]
        (.skip reader (count blob-header))
        (let [[pvalue byte-range] (read-primary-value! reader bytes-read)
              svalues (read-secondary-values! reader)]
          (assoc blob
            :data/primary-bytes byte-range
            :data/values (vec (cons pvalue svalues))
            :data/type nil)))))) ; TODO
