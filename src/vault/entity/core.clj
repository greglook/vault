(ns vault.entity.core
  (:require
    [potemkin :refer [import-vars]]
    (vault.entity
      datom schema tx)))


(import-vars
  (vault.entity.datom
    entity-state)
  (vault.entity.schema
    root?
    update?)
  (vault.entity.tx
    root-record
    root-blob
    update-record
    update-blob
    validate-root-blob
    validate-update-blob))



;; ENTITY RECORD

#_
(defrecord Entity [root-id blob-store indexes]
  clojure.lang.Associative
  clojure.lang.ILookup
  clojure.lang.IPersistentCollection
  clojure.lang.Seqable)
