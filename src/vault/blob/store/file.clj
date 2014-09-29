(ns vault.blob.store.file
  "Content storage backed by a local filesystem."
  (:require
    [byte-streams]
    (clj-time
      [coerce :as coerce-time]
      [core :as time])
    [clojure.java.io :as io]
    [clojure.string :as string]
    (vault.blob
      [content :as content]
      [store :as store]))
  (:import
    java.io.File))


;; ## File System Utilities

(defn- id->file
  "Determines the filesystem path for a blob of content with the given hash
  identifier."
  ^File
  [root id]
  (let [{:keys [algorithm digest] :as id} (content/hash-id id)]
    (io/file root
      (name algorithm)
      (subs digest 0 3)
      (subs digest 3 6)
      (content/path-str id))))


(defn- file->id
  "Reconstructs the hash identifier represented by the given file path."
  [root file]
  (let [root (str root)
        file (str file)]
    (when-not (.startsWith file root)
      (throw (IllegalStateException.
               (str "File " file " is not a child of root directory " root))))
    (-> file
        (subs (inc (count root)))
        (string/split #"/")
        last
        content/parse-id)))


(defn- find-files
  "Walks a directory tree depth first, returning a sequence of files found in
  lexical order."
  [^File path]
  (cond
    (.isFile path) [path]
    (.isDirectory path) (->> path .listFiles sort (map find-files) flatten)
    :else []))


(defn- rm-r
  "Recursively removes a directory of files."
  [^File path]
  (when (.isDirectory path)
    (->> path .listFiles (map rm-r) dorun))
  (.delete path))


(defmacro ^:private when-blob-file
  "An unhygenic macro which binds the blob file to `file` and executes the body
  only if it exists."
  [store id & body]
  `(let [~(with-meta 'file {:tag 'java.io.File})
         (id->file (:root ~store) ~id)]
     (when (.exists ~'file)
       ~@body)))


(defn- blob-stats
  "Calculates storage stats for a blob file."
  [^File file]
  {:stat/size (.length file)
   :stat/stored-at (coerce-time/from-long (.lastModified file))
   :stat/origin (.toURI file)})



;; ## File Store

;; Blob content is stored as files in a multi-level hierarchy under the given
;; root directory.

(defrecord FileBlobStore
  [^File root]

  store/BlobStore

  (enumerate
    [this opts]
    (->> (find-files root)
         (map (partial file->id root))
         (store/select-ids opts)))


  (stat
    [this id]
    (when-blob-file this id
      (merge (content/empty-blob id)
             (blob-stats file))))


  (get*
    [this id]
    (when-blob-file this id
      (-> file
          io/input-stream
          content/read
          (merge (blob-stats file)))))


  (put!
    [this blob]
    (let [{:keys [id content]} blob
          file (id->file root id)]
      (when-not (.exists file)
        (io/make-parents file)
        ; For some reason, io/copy is much faster than byte-streams/transfer here.
        (io/copy content file)
        (.setWritable file false false))
      (merge blob (blob-stats file))))


  (delete!
    [this id]
    (when-blob-file this id
      (.delete file)))


  (erase!!
    [this]
    (rm-r root)))


(defn file-store
  "Creates a new local file-based blob store."
  [root]
  (FileBlobStore. (io/file root)))
