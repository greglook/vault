(ns user
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [puget.printer :as puget :refer [cprint]]
    [vault.blob.core :as blob]
    [vault.data.core :as data]
    [vault.entity.core :as entity]
    (vault.tool
      [config :as tool-conf])))


;; UTILITY FUNCTIONS

(defn cp
  "Prints the last REPL value with Puget."
  []
  (cprint *1))



;; VAULT SYSTEM

(def config
  (->
    {:config tool-conf/default-path}
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
