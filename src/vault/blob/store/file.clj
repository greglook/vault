(ns vault.blob.store.file
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as string]
    (vault.blob
      [core :as blob]
      [store :as store :refer [BlobStore]]))
  (:import
    java.io.File))


;; HELPER FUNCTIONS

(defn- blobref->file
  ^File
  [root blobref]
  (let [blobref (blob/ref blobref)
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
      (blob/ref algorithm (string/join digest)))))


(defn- blob->status-file
  ^File
  [blob]
  (io/file blob "status.edn"))


(defn- blob->content-file
  ^File
  [blob]
  (io/file blob "content"))


(defn- has-content?
  "Checks whether the given blob contains stored content."
  [blob]
  (.exists (blob->content-file blob)))


(defn- blob-status
  "Builds the status map for the given blob."
  [blob]
  (let [content-file (blob->content-file blob)
        status-file (blob->status-file blob)
        status (when (.exists status-file)
                 (edn/read-string (slurp status-file)))]
    (merge status
      {:size (.length content-file)
       :since (java.util.Date. (.lastModified content-file))
       :location (.toURI content-file)})))


; As a long-term idea, this could try to buffer in memory up to a certain
; threshold before spooling to disk.
(defn- spool-tmp-file!
  "Spool input stream to a temporary landing file."
  ^File
  [root content]
  (let [tmp-file (io/file root "tmp" (str "landing-" (System/currentTimeMillis)))]
    (io/make-parents tmp-file)
    (io/copy content tmp-file)
    tmp-file))


(defn- store-blob-files!
  "Stores status and content for a blob. Returns the status map."
  [^File blob
   status
   ^java.io.InputStream stream]
  (let [content-file (blob->content-file blob)
        status-file (blob->status-file blob)]
    (io/make-parents content-file)
    (when-not (empty? status)
      (spit status-file (prn-str status))
      (.setWritable status-file false false))
    (io/copy stream content-file)
    (.setWritable content-file false false)))


(defmacro ^:private for-files
  [[sym dir] expr]
  `(let [files# (->> ~dir .listFiles sort)
         f# (fn [~(vary-meta sym assoc :tag 'java.io.File)] ~expr)]
     (map f# files#)))


(defn- enumerate-files
  "Generates a lazy sequence of file blobs contained in a root directory."
  [^File root]
  ; TODO: intelligently skip entries based on 'start'
  (flatten
    (for-files [algorithm-dir root]
      (for-files [prefix-dir algorithm-dir]
        (for-files [midfix-dir prefix-dir]
          (for-files [blob midfix-dir]
            blob))))))



;; FILE STORE

(defrecord FileBlobStore
  [^File root]

  BlobStore

  (-list [this opts]
    (->> (enumerate-files root)
         (map (partial file->blobref root))
         (store/select-refs opts)))


  (-stat [this blobref]
    (let [blob (blobref->file root blobref)]
      (when (has-content? blob)
        (blob-status blob))))


  (-open [this blobref]
    (let [blob (blobref->file root blobref)]
      (when (has-content? blob)
        [(blob-status blob)
         (io/input-stream (blob->content-file blob))])))


  (-store! [this blobref status stream]
    (let [blob (blobref->file root blobref)]
      (if-not (has-content? blob)
        (store-blob-files! blob status stream))))


  (-remove! [this blobref]
    (let [blob (blobref->file root blobref)]
      (when (.exists blob)
        (.delete (blob->content-file blob))
        (.delete (blob->status-file blob))
        (.delete blob)))))


(defn file-store
  "Creates a new local file-based blob store."
  [root]
  (FileBlobStore. (io/file root)))
