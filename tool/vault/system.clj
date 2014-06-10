(ns vault.system
  (:require
    (clj-time
      [core :as time]
      [format :as ftime])
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [environ.core :refer [env]]
    (puget
      [data]
      [printer :as puget])
    [vault.blob.core :as blob]
    vault.blob.store
    [vault.data.core :as data]
    [vault.entity.core :as entity]
    [vault.index.core :as index]
    (vault.tool
      [config :as config])))


;; GENERAL CONFIG

(puget.data/extend-tagged-map vault.blob.store.Blob 'vault.tool/blob)
(puget.data/extend-tagged-value
  vault.entity.datom.Datom 'vault.tool/datom
  (juxt :op :entity :attribute :value :tx :time))



;; VAULT SYSTEM

(def config
  (config/load-configs (env :vault-config "dev/config")))


(def blobs
  (config/select-blob-store
    (:blob-stores config)
    :default))
