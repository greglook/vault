(ns vault.data.format.edn
  "Functions to handle structured data formatted as EDN."
  (:require
    [clojure.string :as str]
    [puget.data :as edn]
    [puget.printer :as puget])
  (:import
    (java.io
      ByteArrayInputStream
      FilterReader
      InputStream
      InputStreamReader
      PushbackReader
      Reader)
    (java.nio.charset Charset)))


;; CONSTANTS & CONFIGURATION

(def ^Charset blob-charset
  (Charset/forName "UTF-8"))


(def ^:const blob-header
  "Magic header which must appear as the first characters in a data blob."
  "#vault/data\n")


(def ^:private ^:const blob-width
  "Width of text to use in serialized blobs."
  100)


(defn data-type
  "Determines the 'type' of the given value. By default, the result is just the
  class of the value. For maps, the :vault.data/type key is used if present.
  This is the main way types are represented in the data layer."
  [v]
  (or (when (map? v)
        (:vault.data/type v))
      (class v)))



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


(defn print-blob
  "Prints the given data value(s) as canonical EDN in a data blob."
  [value & more]
  (binding [puget/*strict-mode* true]
    (print (puget/color-text :tag blob-header))
    (print-value value)
    (doseq [v more]
      (newline)
      (print-value v))))


(defn print-blob-str
  "Prints a canonical EDN representation to a string and returns it. This
  function disables colorization."
  [value & more]
  (binding [puget/*colored-output* false]
    (str/trim (with-out-str (apply print-blob value more)))))



;; DESERIALIZATION

(defn- counting-reader
  "Wraps the given input stream with a proxy which counts the bytes read
  through it. The second argument should be an atom containing a counter."
  [^Reader reader
   counter]
  (proxy [FilterReader] [reader]
    (read []
      (let [c (.read reader)]
        (when-not (= c -1)
          (let [len (count (.getBytes (str (char c)) blob-charset))]
            (swap! counter + len)))
        c))
    (skip [n]
      ; NOTE: this assumes that all skipped characters are ASCII, since
      ; currently this is only used for skipping the blob header.
      (swap! counter + n)
      (.skip reader n))))


(defn- check-header
  "Reads the first few bytes from a blob's content to determine whether it is a
  data blob. The result is true if the header matches, otherwise false."
  [^bytes content]
  (let [magic-bytes (.getBytes blob-header blob-charset)
        magic-len (count magic-bytes)]
    (= (seq magic-bytes) (take magic-len (seq content)))))


(defn- read-primary-value!
  "Reads the primary EDN value from a data blob. Returns a vector containing
  the primary value and the range of bytes as a second vector."
  [reader bytes-read]
  (let [byte-start @bytes-read
        value (edn/read {:readers {}} reader)
        byte-range [byte-start @bytes-read]]
    [value byte-range]))


(defn- read-secondary-values!
  "Reads the secondary EDN values from a data blob. Returns a seq of the values
  read."
  [reader]
  (let [opts {:eof ::end-stream
              :readers {}}
        read-stream (partial edn/read opts reader)
        edn-stream (repeatedly read-stream)
        not-eos? (partial not= ::end-stream)]
    (doall (take-while not-eos? edn-stream))))


(defn read-blob
  "Reads the contents of the given blob and attempts to parse it as an EDN data
  structure. Returns an updated blob record, or nil if the content is not EDN."
  [blob]
  (when (check-header (:content blob))
    (let [bytes-read (atom 0)]
      (with-open [reader (-> (:content blob)
                             ByteArrayInputStream.
                             (InputStreamReader. blob-charset)
                             (counting-reader bytes-read)
                             PushbackReader.)]
        (.skip reader (count blob-header))
        (let [[pvalue byte-range] (read-primary-value! reader bytes-read)
              svalues (read-secondary-values! reader)]
          (assoc blob
            :data/primary-bytes byte-range
            :data/values (vec (cons pvalue svalues))
            :data/type (data-type pvalue)))))))


(defn primary-bytes
  "Utility function which takes a data blob and returns only the bytes in the
  primary value. If the bob does not contain a :data/primary-bytes key, the
  blob content is returned as-is."
  [blob]
  (if-let [byte-range (:data/primary-bytes blob)]
    (java.util.Arrays/copyOfRange (:content blob)
                                  (first byte-range)
                                  (second byte-range))
    (:content blob)))
