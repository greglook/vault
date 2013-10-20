(ns mvxcvi.vault.blob.store.file
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [mvxcvi.vault.blob :as blob]
    [mvxcvi.vault.blob.store :refer :all]))


;; HELPER FUNCTIONS

(defn- blobref->file
  [root blobref]
  (let [blobref (blob/blob-ref blobref)
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
      (blob/blob-ref algorithm (string/join digest)))))



;; FILE STORE

(defrecord FileBlobStore
  [root algorithm]

  BlobStore

  (enumerate [this]
    (->>
      (for [algorithm-dir (.listFiles root)]
        (for [prefix-dir (.listFiles algorithm-dir)]
          (for [midfix-dir (.listFiles prefix-dir)]
            (seq (.listFiles midfix-dir)))))
      flatten
      (map (partial file->blobref root))))


  (content-stream [this blobref]
    (let [file (blobref->file root blobref)]
      (if (.exists file)
        (io/input-stream file)
        nil)))


  (store-content! [this content]
    (let [blobref (blob/hash-content algorithm content)
          file (blobref->file root blobref)]
      (io/make-parents file)
      (io/copy content file)
      blobref))


  (blob-info [this blobref]
    (let [file (blobref->file root blobref)]
      {:location (.toURI file)
       :size (.length file)})))


(defn file-store
  "Creates a new local file-based blobstore."
  ([root] (file-store root :sha256))
  ([root algorithm] (FileBlobStore. (io/file root) algorithm)))
