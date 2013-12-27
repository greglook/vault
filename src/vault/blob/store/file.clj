(ns vault.blob.store.file
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [vault.blob.core :as blob :refer [BlobStore]])
  (:import
    java.io.File
    java.util.Date))


;; HELPER FUNCTIONS

(defn- id->file
  ^File
  [root id]
  (let [id (blob/hash-id id)
        {:keys [algorithm digest]} id]
    (io/file root
      (name algorithm)
      (subs digest 0 3)
      (subs digest 3 6)
      (subs digest 6))))


(defn- file->id
  [root file]
  (let [root (str root)
        file (str file)]
    (when-not (.startsWith file root)
      (throw (IllegalArgumentException.
               (str "File " file " is not a child of root directory " root))))
    (let [[algorithm & digest] (-> file
                                   (subs (inc (count root)))
                                   (string/split #"/"))]
      (blob/hash-id algorithm (string/join digest)))))


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



;; FILE STORE

(defrecord FileBlobStore
  [^File root])

; Don't know why it has to be done this way, but if it's defined inline then
; cloverage breaks.
(extend-protocol BlobStore
  FileBlobStore

  (enumerate [this opts]
    (->> (enumerate-files (:root this))
         (map (partial file->id (:root this)))
         (blob/select-ids opts)))


  (stat [this id]
    (when-blob-file this id
      {:size (.length file)
       :stored-at (Date. (.lastModified file))
       :location (.toURI file)}))


  (open [this id]
    (when-blob-file this id
      (io/input-stream file)))


  (store! [this blob]
    (let [{:keys [id content]} blob
          file (id->file (:root this) id)]
      (when-not (.exists file)
        (io/make-parents file)
        ; For some reason, io/copy is much faster than byte-streams/transfer here.
        (io/copy (byte-streams/to-input-stream content) file)
        (.setWritable file false false))))


  (delete! [this id]
    (when-blob-file this id
      (.delete file)))


  (destroy!! [this]
    (let [rm-r (fn rm-r [^File path]
                 (when (.isDirectory path)
                   (doseq [child (.listFiles path)]
                     (rm-r child)))
                 (.delete path))]
      (rm-r (:root this)))))


(defn file-store
  "Creates a new local file-based blob store."
  [root]
  (FileBlobStore. (io/file root)))