(ns vault.system
  "Functions for loading Vault system configuration from a file."
  (:require
    [clj-time.core :as time]
    [clojure.java.io :as io]
    [com.stuartsierra.component :as component]
    [environ.core :refer [env]])
  (:import
    java.io.File))


;; ## System Lifecycle

(def core
  (component/system-map))


(defn start!
  "Start the Vault system."
  []
  (when core
    (alter-var-root #'core component/start))
  :start)


(defn stop!
  "Stops the Vault system."
  []
  (when core
    (alter-var-root #'core component/stop))
  :stop)



;; ## Configuration Files

(defn- clj-file?
  "Determines whether the given file is a clojure file."
  [^File file]
  (and (.isFile file)
       (.matches (.getName file) ".*\\.clj$")))


(defn include
  "Include another config file or directory. If the path points to a directory,
  all files with names ending in `.clj` within it will be loaded recursively."
  [path]
  (let [file (io/file path)]
    (binding [*ns* (find-ns 'vault.system)]
      (if (.isDirectory file)
        (doseq [f (file-seq file)]
          (when (clj-file? f)
            (load-file (str f))))
        (load-file path)))))


(defn component
  "Registers a component in the system map."
  ([k v]
   (alter-var-root #'core assoc k v))
  ([k v deps]
   (component k (component/using v deps))))


(defn components
  "Registers a collection of components in the system map. May be passed a map
  of keys to components, or a sequence of key/component pairs."
  [& more]
  (let [comps (if (and (= 1 (count more))
                       (map? (first more)))
                (first more)
                (apply array-map more))]
     (alter-var-root #'core merge comps)))


(defn defaults
  "Select default components by supplying a map of key-value pairs."
  [& ks]
  (component :defaults (apply hash-map ks)))
