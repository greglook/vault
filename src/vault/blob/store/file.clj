(ns vault.blob.store.file
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [clojure.string :as string]
    (clj-time
      [coerce :as ctime]
      [core :as time])
    (vault.blob
      [digest :as digest]
      [store :as store]))
  (:import
    java.io.File))


;;;;; HELPER FUNCTIONS ;;;;;

(defn- id->file
  ^File
  [root id]
  (let [id (digest/hash-id id)
        {:keys [algorithm digest]} id]
    (io/file root
      (name algorithm)
      (subs digest 0 3)
      (subs digest 3 6)
      (digest/path-str id))))


(defn- file->id
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
        digest/parse-id)))


(defmacro ^:private for-files
  [[sym dir] expr]
  `(let [files# (->> ~dir .listFiles sort)
         f# (fn [~(vary-meta sym assoc :tag 'java.io.File)] ~expr)]
     (map f# files#)))


(defn- enumerate-files
  "Generates a lazy sequence of blob files contained in a root directory."
  [^File root]
  ; TODO: intelligently skip entries based on 'after'
  (flatten
    (for-files [algorithm-dir root]
      (for-files [prefix-dir algorithm-dir]
        (for-files [midfix-dir prefix-dir]
          (for-files [blob midfix-dir]
            blob))))))


(defmacro ^:private when-blob-file
  "This is an unhygenic macro which binds the blob file to 'file' and executes
  the body only if it exists."
  [store id & body]
  `(let [~(with-meta 'file {:tag 'java.io.File})
         (id->file (:root ~store) ~id)]
     (when (.exists ~'file)
       ~@body)))


(defn- blob-stats
  "Calculates statistics for a blob file."
  [^File file]
  {:stat/size (.length file)
   :stat/stored-at (ctime/from-long (.lastModified file))
   :stat/origin (.toURI file)})



;;;;; FILE STORE ;;;;;

(defrecord FileBlobStore
  [^File root])

(extend-type FileBlobStore
  store/BlobStore

  (enumerate [this opts]
    (->> (enumerate-files (:root this))
         (map (partial file->id (:root this)))
         (store/select-ids opts)))


  (stat [this id]
    (when-blob-file this id
      (merge (store/record id)
             (blob-stats file))))


  (get* [this id]
    (when-blob-file this id
      (-> file
          io/input-stream
          store/load
          (merge (blob-stats file)))))


  (put! [this blob]
    (let [{:keys [id content]} blob
          file (id->file (:root this) id)]
      (when-not (.exists file)
        (io/make-parents file)
        ; For some reason, io/copy is much faster than byte-streams/transfer here.
        (io/copy content file)
        (.setWritable file false false))
      (merge blob (blob-stats file))))


  store/DestructableBlobStore

  (delete!
    [this id]
    (when-blob-file this id
      (.delete file)))


  (destroy!!
    [this]
    (let [rm-r (fn rm-r [^File path]
                 (when (.isDirectory path)
                   (->> path .listFiles (map rm-r) dorun))
                 (.delete path))]
      (rm-r (:root this)))))


(defn file-store
  "Creates a new local file-based blob store."
  [root]
  (FileBlobStore. (io/file root)))
