(ns vault.index.core
  (:require
    [potemkin :refer [import-vars]]
    (vault.index
      [catalog :as catalog]
      [search :as search])))


(import-vars
  (vault.index.catalog
    catalog)
  (vault.index.search
    update!
    search))
