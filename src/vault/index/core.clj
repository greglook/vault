(ns vault.index.core
  (:require
    [potemkin :refer [import-vars]]
    (vault.index
      [engine :as engine])))


(import-vars
  (vault.index.engine
    init!
    update!
    search))
