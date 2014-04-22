(ns vault.data.format.edn
  "Functions to handle structured data formatted as EDN."
  (:require
    [clojure.string :as str]
    [clojure.edn :as edn]
    (puget
      [data :as data]
      [printer :as puget])
    [vault.blob.core :as blob])
  (:import
    (java.io
      ByteArrayInputStream
      ByteArrayOutputStream
      FilterReader
      InputStream
      InputStreamReader
      OutputStreamWriter
      PushbackReader
      Reader)
    (java.nio.charset Charset)
    vault.blob.core.HashID))


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



;; TAGGED VALUES

(def data-readers
  "Atom containing a map of tag readers supported by Vault."
  (atom
    {'bin data/read-bin
     'uri data/read-uri
     ; TODO: 'inst data/read-inst-cljtime
     'vault/ref blob/parse-id}
    :validator map?))


(defn register-reader!
  "Registers a function as the data reader for an EDN tag."
  [tag f]
  {:pre [(symbol? tag)]}
  (swap! data-readers assoc tag f))


(data/extend-tagged-str HashID vault/ref)



;; SERIALIZATION

(defn- print-value
  "Prints the canonical EDN representation for the given Clojure value."
  [value]
  (let [opts {:width blob-width}]
    (puget/pprint value opts)))


(defn- edn-str
  "Returns a trimmed string of the canonical EDN representation for the given
  Clojure value."
  [value]
  (binding [puget/*colored-output* false
            puget/*strict-mode* true]
    (str/trim (with-out-str (print-value value)))))


(defn edn-blob
  "Constructs a data blob from a value. The second argument may be a function
  which takes the bytes comprising the rendered primary value and returns a
  sequence of secondary data values."
  ([value]
   (edn-blob value nil))
  ([value f]
   (let [value-str (edn-str value)
         secondary-values (when f (f (.getBytes value-str blob-charset)))
         content-bytes (ByteArrayOutputStream.)
         byte-range (atom [])]
     (with-open [content (OutputStreamWriter. content-bytes blob-charset)]
       (.write content blob-header)
       (.flush content)
       (swap! byte-range conj (.size content-bytes))
       (.write content value-str)
       (.flush content)
       (swap! byte-range conj (.size content-bytes))
       (doseq [v secondary-values]
         (.write content "\n\n")
         (.write content (edn-str v))))
     (assoc (blob/load (.toByteArray content-bytes))
       :data/primary-bytes @byte-range
       :data/values (vec (cons value secondary-values))
       :data/type (data-type value)))))


(defn print-blob
  "Prints the values from the given blob. This can be used to pretty-print a
  colorized version of the values in an EDN data blob."
  [blob]
  (when-let [values (:data/values blob)]
    (print (puget/color-text :tag blob-header))
    (print-value (first values))
    (doseq [v (rest values)]
      (newline)
      (print-value v))))



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
        value (edn/read {:readers @data-readers} reader)
        byte-range [byte-start @bytes-read]]
    [value byte-range]))


(defn- read-secondary-values!
  "Reads the secondary EDN values from a data blob. Returns a seq of the values
  read."
  [reader]
  (let [opts {:eof ::end-stream
              :readers @data-readers}
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
  primary value. If the blob does not contain a :data/primary-bytes key, the
  blob content is returned as-is."
  [blob]
  (if-let [byte-range (:data/primary-bytes blob)]
    (java.util.Arrays/copyOfRange (:content blob)
                                  (first byte-range)
                                  (second byte-range))
    (:content blob)))
