(ns vault.data.edn
  "Functions to handle structured data formatted as EDN."
  (:refer-clojure :exclude [type])
  (:require
    (clj-time
      [core :as time]
      [format :as ftime])
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
    java.nio.charset.Charset
    org.joda.time.DateTime
    vault.blob.digest.HashID))


;;;;; CONSTANTS & CONFIGURATION ;;;;;

(def ^:private ^Charset data-charset
  (Charset/forName "UTF-8"))


(def ^:const ^:private data-header
  "Magic header which must appear as the first characters in a data blob."
  "#vault/data\n")


(def ^:const ^:private print-opts
  "Options to use to render EDN values with Puget."
  {:width 100
   :strict true
   :map-delimiter ""
   :map-coll-separator :line
   :print-meta false})


(def ^:const type-key
  "Keyword which defines a map's data type."
  :vault/type)


(defn type
  "Determines the 'type' of the given value. By default, the result is just the
  class of the value. For maps, the type-key is used if present. This is the
  main way types are represented in the data layer."
  [value]
  (cond
    (map? value) (get value type-key :map)
    (set? value) :set
    (vector? value) :vector
    (sequential? value) :list
    :else (class value)))


(defn typed-map
  "Constructs a new value with the given data type."
  [t & entries]
  (apply hash-map type-key t entries))



;;;;; TAGGED VALUES ;;;;;

(def ^:no-doc data-readers
  "Atom containing a map of tag readers supported by Vault."
  (atom
    {'bin data/read-bin
     'uri data/read-uri}
    :validator map?))


(defmacro register-tag!
  "Registers a function as the data reader for an EDN tag."
  ([tag reader]
   `(do
      #_
      (puget/with-color
        (println
          (str "Reading " (puget/color-text :tag (str \# '~tag))
               " values with " (puget/color-text :function-symbol (str '~reader)))))
      (swap! data-readers assoc '~tag ~reader)))
  ([tag t writer reader]
   `(do
      #_
      (puget/with-color
        (println
          (str "Writing " (puget/color-text :class-name (str '~t))
               " values as " (puget/color-text :tag (str \# '~tag))
               " with " (puget/color-text :function-symbol (str '~writer))
               " read by " (puget/color-text :function-symbol (str '~reader)))))
      (data/extend-tagged-value ~t '~tag ~writer)
      (swap! data-readers assoc '~tag ~reader))))


(register-tag! vault/ref
  HashID str
  blob/parse-id)


(register-tag! inst
  DateTime
  (partial ftime/unparse (ftime/formatters :date-time))
  (partial ftime/parse   (ftime/formatters :date-time)))



;;;;; SERIALIZATION ;;;;;

(defn- print-value
  "Prints the canonical EDN representation for the given Clojure value."
  [value]
  (puget/pprint value print-opts))


(defn- edn-str
  "Returns a trimmed string of the canonical EDN representation for the given
  Clojure value."
  ^String
  [value]
  (let [opts (assoc print-opts :print-color false)]
    (puget/pprint-str value opts)))


(defn data->blob
  "Constructs a data blob from a value. The second argument may be a function
  which takes the bytes comprising the rendered primary value and returns a
  sequence of secondary data values."
  ([value]
   (data->blob value nil))
  ([value f]
   (let [value-str (edn-str value)
         secondary-values (when f (f (.getBytes value-str data-charset)))
         content-bytes (ByteArrayOutputStream.)
         byte-range (long-array 2)]
     (with-open [content (OutputStreamWriter. content-bytes data-charset)]
       (.write content data-header)
       (.flush content)
       (aset byte-range 0 (.size content-bytes))
       (.write content value-str)
       (.flush content)
       (aset byte-range 1 (.size content-bytes))
       (doseq [v secondary-values]
         (.write content "\n\n")
         (.write content (edn-str v))))
     (assoc (blob/read (.toByteArray content-bytes))
       :data/primary-bytes (vec byte-range)
       :data/values (vec (cons value secondary-values))
       :data/type (type value)))))


(defn print-blob
  "Prints the values from the given blob. This can be used to pretty-print a
  colorized version of the values in an EDN data blob."
  [blob]
  (when-let [values (:data/values blob)]
    (print (puget/color-text :tag data-header))
    (print-value (first values))
    (doseq [v (rest values)]
      (newline)
      (print-value v))))



;;;;; DESERIALIZATION ;;;;;

(defn- counting-reader
  "Wraps the given input stream with a proxy which counts the bytes read
  through it. The second argument should be an atom containing a counter."
  [^Reader reader
   counter]
  (proxy [FilterReader] [reader]
    (read []
      (let [c (.read reader)]
        (when-not (= c -1)
          (let [len (count (.getBytes (str (char c)) data-charset))]
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
  (let [header (.getBytes data-header data-charset)
        len (count header)]
    (= (seq header) (take len (seq content)))))


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
                             (InputStreamReader. data-charset)
                             (counting-reader bytes-read)
                             PushbackReader.)]
        (.skip reader (count data-header))
        (let [[pvalue byte-range] (read-primary-value! reader bytes-read)
              svalues (read-secondary-values! reader)]
          (assoc blob
            :data/primary-bytes byte-range
            :data/values (vec (cons pvalue svalues))
            :data/type (type pvalue)))))))


(defn primary-bytes
  "Utility function which takes a data blob and returns only the bytes in the
  primary value. If the blob does not contain a :data/primary-bytes key, the
  blob content is returned as-is."
  [blob]
  (let [^bytes content (:content blob)]
    (if-let [[^long start ^long end] (:data/primary-bytes blob)]
      (java.util.Arrays/copyOfRange content start end)
      content)))
