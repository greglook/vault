(ns user
  (:require
    (clj-time
      [core :as time]
      [format :as ftime])
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [puget.printer :as puget]
    [vault.blob.core :as blob]
    [vault.data.core :as data]
    [vault.entity.core :as entity]
    [vault.index.core :as index]
    (vault.tool
      [config :as tool-conf])))


;; VAULT SYSTEM

(def config
  (->
    {:config-dir tool-conf/default-path}
    tool-conf/initialize))


(def blobs
  (tool-conf/select-blob-store
    (:blob-stores config)
    :default))



;; LIFECYCLE FUNCTIONS

(defn start
  "Start the application."
  []
  nil)


(defn stop
  "Stop the application."
  []
  nil)


(defn reset
  "Reset the repl and reload state."
  []
  (stop)
  (refresh :after 'user/start))
