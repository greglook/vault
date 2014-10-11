(ns user
  (:require
    (clj-time
      [core :as time]
      [format :as timef])
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [com.stuartsierra.component :as component]
    [environ.core :refer [env]]
    [puget.printer :as puget]
    (vault.blob
      [content :as content]
      [store :as store])
    [vault.entity.datom]
    [vault.system :as sys]))


;; GENERAL CONFIG

(puget.data/extend-tagged-map vault.blob.content.Blob 'vault.tool/blob)
(puget.data/extend-tagged-value
  vault.entity.datom.Datom 'vault.tool/datom
  (juxt :op :entity :attribute :value :tx :time))



;; VAULT SYSTEM

(defn go!
  "Initializes with the default config and starts the system."
  []
  (sys/include (env :vault-config "dev/config/dev.clj"))
  (sys/start!))


(defn reload!
  "Reloads all changed namespaces to update code, then re-launches the system."
  []
  (sys/stop!)
  (refresh :after 'user/go!))
