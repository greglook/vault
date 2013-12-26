(ns vault.blob.store.file
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    (vault.blob
      [core :as blob :refer [BlobStore]]
      [digest :as digest]))
  (:import
    (java.io
      File)))


;; HELPER FUNCTIONS

(defn- hashid->file
  ^File
  [root id]
  (let [id (digest/hash-id id)
        {:keys [algorithm digest]} id]
    (io/file root
             (name algorithm)
             (subs digest 0 3)
             (subs digest 3 6)
             (subs digest 6))))


(defn- file->hashid
  [root file]
  (let [root (str root)
        file (str file)]
    (when-not (.startsWith file root)
      (throw (IllegalArgumentException.
               (str "File " file " is not a child of root directory " root))))
    (let [[algorithm & digest] (-> file
                                   (subs (inc (count root)))
                                   (string/split #"/"))]
      (digest/hash-id algorithm (string/join digest)))))


(defmacro ^:private for-files
  [[sym dir] expr]
  `(let [files# (->> ~dir .listFiles sort)
         f# (fn [~(vary-meta sym assoc :tag 'java.io.File)] ~expr)]
     (map f# files#)))


(defn- enumerate-files
  "Generates a lazy sequence of file blobs contained in a root directory."
  [^File root]
  ; TODO: intelligently skip entries based on 'after'
  (flatten
    (for-files [algorithm-dir root]
      (for-files [prefix-dir algorithm-dir]
        (for-files [midfix-dir prefix-dir]
          (for-files [blob midfix-dir]
            blob))))))



;; FILE STORE

(defrecord FileBlobStore
  [^File root])


(extend-protocol BlobStore
  FileBlobStore

  (-list [this opts]
    (->> (enumerate-files (:root this))
         (map (partial file->hashid (:root this)))
         (digest/select-ids opts)))


  (-stat [this id]
    (let [file (hashid->file (:root this) id)]
      (when (.exists file)
        {:size (.length file)
         :stored-at (java.util.Date. (.lastModified file))
         :location (.toURI file)})))


  (-open [this id]
    (let [file (hashid->file (:root this) id)]
      (when (.exists file)
        (io/input-stream file))))


  (-store! [this blob]
    (let [{:keys [id content]} blob
          file (hashid->file (:root this) id)]
      (when-not (.exists file)
        (io/make-parents file)
        (io/copy content file)
        (.setWritable file false false))))


  (-remove! [this id]
    (let [file (hashid->file (:root this) id)]
      (when (.exists file)
        (.delete file)))))


(defn file-store
  "Creates a new local file-based blob store."
  [root]
  (->FileBlobStore (io/file root)))
