(ns mvxcvi.vault.blob.store.file
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [mvxcvi.vault.blob :refer :all]
    [mvxcvi.vault.blob.store :refer :all]))


;; HELPER FUNCTIONS

(defn- blobref->file
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
  [file]
  (let [result (shell/sh "file"
                         "--brief"
                         "--mime"
                         (.getAbsolutePath file))]
    (when (= 0 (:exit result))
      (string/trim (:out result)))))



;; FILE STORE

(defrecord FileBlobStore
  [algorithm root]

  BlobStore

  (enumerate [this]
    (enumerate this {}))

  (enumerate
    [this opts]
    ; TODO: intelligently skip entries based on 'start'
    (let [blobrefs (for [algorithm-dir (sort (.listFiles root))]
                     (for [prefix-dir (sort (.listFiles algorithm-dir))]
                       (for [midfix-dir (sort (.listFiles prefix-dir))]
                         (seq (sort (.listFiles midfix-dir))))))
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
