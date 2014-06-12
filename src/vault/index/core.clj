(ns vault.index.core
  (:require
    [vault.blob.store :as store :refer [BlobStore]]))


(defprotocol Index

  (search
    [this pattern opts]
    "Search the index for records with values matching the given pattern.
    Options may include:
    - :ascending   Whether to return the sequence in ascending order.")

  (update
    [this record]
    "Return an updated version of the index with information about the record."))


(defrecord MemoryIndex [attrs index])

(extend-type MemoryIndex
  Index

  (update
    [{:keys [attrs index] :as this} record]
    (let [key-vec (vec (map record attrs))]
      (when (some nil? key-vec)
        (throw (IllegalArgumentException.
                 (str "Cannot update index with record missing required "
                      "attributes " (pr-str attrs) " " (pr-str record)))))
      (->>
        (->
          index
          (get key-vec (hash-set))
          (conj record))
        (assoc index key-vec)
        (assoc this :index)))))


(defn memory-index
  [attr & attrs]
  (MemoryIndex. (vec (cons attr attrs)) (sorted-map)))



#_
{:blob      HashID      ; blob hash-id
 :size      Long        ; blob byte length
 :type      Keyword     ; data type
 :stored-at DateTime}   ; time added to index

#_
(extend-type BlobIndex
  Index

  (search
    [this prefix]
    ()

    )

  BlobStore

  (enumerate
    [this opts]
    nil)

  (stat
    [this id]
    nil)

  (put!
    [this blob]

    nil))



#_
(defn changes-by
  [indexes pk-id]
  (let [roots   (index/rsearch (:ref/to indexes) [pk-id :vault.entity/root])
        updates (index/rsearch (:ref/to indexes) [pk-id :vault.entity/update])]
    (rmerge-sort-by :time roots updates)))
