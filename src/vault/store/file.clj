(ns vault.store.file
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [clojure.string :as string]
    [vault.blob :refer :all]
    [vault.store :refer :all]))


;; HELPER FUNCTIONS

(defn- blobref->file
  ^java.io.File
  [root blobref]
  (let [blobref (make-blobref blobref)
        {:keys [algorithm digest]} blobref]
    (io/file root
             (name algorithm)
             (subs digest 0 3)
             (subs digest 3 6)
             (subs digest 6))))


(defn- file->blobref
  [root file]
  (let [root (str root)
        file (str file)]
    (when-not (.startsWith file root)
      (throw (IllegalArgumentException.
               (str "File " file " is not a child of root directory " root))))
    (let [[algorithm & digest] (-> file
                                   (subs (inc (count root)))
                                   (string/split #"/"))]
      (make-blobref algorithm (string/join digest)))))


(defn- file-content-type
  "Attempts to use the `file` command to provide content-type information for a
  stored blob. Returns a MIME string on success."
  [^java.io.File file]
  ; TODO: check out java.nio.file.Files/probeContentType
  (let [result (shell/sh "file"
                         "--brief"
                         "--mime"
                         (.getAbsolutePath file))]
    (when (= 0 (:exit result))
      (string/trim (:out result)))))


(defmacro ^:private for-files
  [[sym dir] expr]
  `(let [files# (->> ~dir .listFiles sort)]
     (for [~(vary-meta sym assoc :tag 'java.io.File) files#]
       ~expr)))


(defn- enumerate-files
  "Generates a lazy sequence of file blobs contained in a root directory."
  [^java.io.File root]
  ; TODO: intelligently skip entries based on 'start'
  (flatten
    (for-files [algorithm-dir root]
      (for-files [prefix-dir algorithm-dir]
        (for-files [midfix-dir prefix-dir]
          (for-files [blob midfix-dir]
            blob))))))



;; FILE STORE

(defrecord FileBlobStore
  [algorithm
   ^java.io.File root]

  BlobStore

  (algorithm [this]
    algorithm)


  (enumerate [this]
    (enumerate this {}))

  (enumerate [this opts]
    (->> (enumerate-files root)
         (map (partial file->blobref root))
         (select-blobrefs opts)))


  (stat [this blobref]
    (let [file (blobref->file root blobref)]
      (when (.exists file)
        {:size (.length file)
         :since (java.util.Date. (.lastModified file))
         :content-type (file-content-type file)
         :location (.toURI file)})))


  (open [this blobref]
    (let [file (blobref->file root blobref)]
      (when (.exists file)
        (io/input-stream file))))


  (store! [this content]
    (let [tmp (io/file root "tmp"
                       (str "landing-" (System/currentTimeMillis)))]
      (io/make-parents tmp)
      (io/copy content tmp)
      (let [blobref (hash-content algorithm tmp)
            file (blobref->file root blobref)]
        (io/make-parents file)
        (when-not (.renameTo tmp file)
          (throw (RuntimeException.
                   (str "Failed to rename landing file " tmp
                        " to stored blob " file))))
        blobref)))


  (remove! [this blobref]
    (let [file (blobref->file root blobref)]
      (when (.exists file)
        (.delete file)))))


(defn file-store
  "Creates a new local file-based blob store."
  [algorithm root]
  (FileBlobStore. algorithm
                  (io/file root)))
