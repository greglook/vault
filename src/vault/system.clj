(ns vault.system
  "Functions for loading Vault system configuration from a file."
  (:require
    [clj-time.core :as time]
    [clojure.java.io :as io]
    [com.stuartsierra.component :as component]
    [environ.core :refer [env]]))


(def core
  (component/system-map))


(defn component
  "Registers a component in the system map."
  ([k v]
   (alter-var-root #'core assoc k v))
  ([k v deps]
   (component k (component/using v deps))))


(defn defaults
  "Select default components by supplying a map of key-value pairs."
  [& ks]
  (component :defaults (apply hash-map ks)))


; TODO: see riemann.config/include
