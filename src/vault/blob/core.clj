(ns vault.blob.core
  (:refer-clojure :exclude [get hash list load])
  (:require
    [potemkin :refer [import-vars]]
    (vault.blob
      digest
      store)))


(import-vars
  (vault.blob.digest
    hash
    path-str
    parse-id
    hash-id)
  (vault.blob.store
    record
    load
    list
    stat
    get
    get'
    put!
    store!
    delete!))
