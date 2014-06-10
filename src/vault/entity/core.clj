(ns vault.entity.core
  (:require
    [potemkin :refer [import-vars]]
    (vault.entity
      datom tx)))


(import-vars
  (vault.entity.datom
    datom
    entity-state)
  (vault.entity.tx
    root-record
    root-blob
    root?
    update-record
    update-blob
    update?
    validate-root-blob
    validate-update-blob))



;; ENTITY RECORD

#_
(defrecord Entity [root-id blob-store indexes]
  clojure.lang.Associative
  clojure.lang.ILookup
  clojure.lang.IPersistentCollection
  clojure.lang.Seqable)
