(ns vault.index.core
  (:require
    [potemkin :refer [import-vars]]
    (vault.index
      [catalog :as catalog]
      [engine :as engine])))


(import-vars
  (vault.index.catalog
    catalog)
  (vault.index.engine
    init!
    update!
    search))
