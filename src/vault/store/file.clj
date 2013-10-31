(ns vault.store.file
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
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
  (let [result (shell/sh "file"
                         "--brief"
                         "--mime"
                         (.getAbsolutePath file))]
    (when (= 0 (:exit result))
      (string/trim (:out result)))))


(defmacro ^:private for-files
  [[sym dir] expr]
  `(for [~sym (sort (.listFiles ^java.io.File ~dir))]
     ~expr))



;; FILE STORE

(defrecord FileBlobStore
  [algorithm
   ^java.io.File root]

  BlobStore

  (enumerate [this]
    (enumerate this {}))

  (enumerate
    [this opts]
    ; TODO: intelligently skip entries based on 'start'
    (let [blobrefs (for-files [algorithm-dir root]
                     (for-files [prefix-dir algorithm-dir]
                       (for-files [midfix-dir prefix-dir]
                         (for-files [blob midfix-dir]
                           blob))))
          blobrefs (map (partial file->blobref root) (flatten blobrefs))
          blobrefs (if-let [start (:start opts)]
                     (drop-while #(< 0 (compare start (str %))) blobrefs)
                     blobrefs)
          blobrefs (if-let [cnt (:count opts)]
                     (take cnt blobrefs)
                     blobrefs)]
      blobrefs))


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
        blobref))))


(defn file-store
  "Creates a new local file-based blobstore."
  ([algorithm root] (FileBlobStore. algorithm (io/file root))))
