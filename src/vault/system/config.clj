(ns vault.system.config
  "Functions implementing the DSL for system configuration files."
  (:require
    [com.stuartsierra.component :as component]
    [environ.core :refer [env]]
    [vault.system.core :as core]))


(defn component
  "Registers a component in the system map."
  ([k v]
   (alter-var-root #'core/system assoc k v))
  ([k v deps]
   (component k (component/using v deps))))


(defn components
  "Registers a collection of components in the system map. May be passed a map
  of keys to components, or a sequence of key/component pairs."
  [& args]
  (let [comps (if (and (= 1 (count args))
                       (map? (first args)))
                (first args)
                (apply array-map args))]
     (alter-var-root #'core/system merge comps)))


(defn defaults
  "Select default components by supplying a map of key-value pairs."
  [& ks]
  (component :defaults (apply hash-map ks)))
