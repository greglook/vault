(ns vault.system
  (:require
    (clj-time
      [core :as time]
      [format :as timef])
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [environ.core :refer [env]]
    [puget.printer :as puget]
    [vault.blob.core :as blob]
    [vault.data.core :as data]
    [vault.entity.core :as entity]
    [vault.index.core :as index]
    (vault.tool
      [config :as conf])))


;; GENERAL CONFIG

(puget.data/extend-tagged-map vault.blob.store.Blob 'vault.tool/blob)
(puget.data/extend-tagged-value
  vault.entity.datom.Datom 'vault.tool/datom
  (juxt :op :entity :attribute :value :tx :time))



;; VAULT SYSTEM

#_
{:blob-stores {:default :local
               :local (file-store "/home/...")}
 :catalog (index/catalog {:blobs ...} :blobs)
 :sig-provider (crypto/keyring-provider ...)}


(def config
  (conf/load-configs (env :vault-config "dev/config")))


(def blobs
  (conf/select-blob-store
    (:blob-stores config)
    :default))
