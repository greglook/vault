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
    [vault.entity.datom]))


;; GENERAL CONFIG

(puget.data/extend-tagged-map vault.blob.content.Blob 'vault.tool/blob)
(puget.data/extend-tagged-value
  vault.entity.datom.Datom 'vault.tool/datom
  (juxt :op :entity :attribute :value :tx :time))



;; VAULT SYSTEM

(def system nil)


(defn init!
  "Initialize the Vault system."
  [config-path]
  ; TODO: implement
  :init)


(defn start!
  "Start the Vault system."
  []
  (when system
    (alter-var-root #'system component/start))
  :start)


(defn go!
  "Initializes with the default config and starts the system."
  []
  (init! (env :vault-config "dev/config"))
  (start!))


(defn stop!
  "Stops the wonderdome system."
  []
  (when system
    (alter-var-root #'system component/stop))
  :stop)


(defn reload!
  "Reloads all changed namespaces to update code, then re-launches the system."
  []
  (stop!)
  (refresh :after 'user/go!))
