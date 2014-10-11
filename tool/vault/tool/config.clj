(ns vault.tool.config
  (:require
    [clojure.java.io :as io]
    [clojure.stacktrace :refer [print-cause-trace]]
    [puget.printer :refer [cprint]]))


;; CONFIGURATION FILE LOADING

(defn- load-config-file
  "Reads a Clojure configuration file. The given require forms will be loaded
  and made available to the configuration."
  [path requirements]
  (let [file (io/file path)]
    (when (.exists file)
      (let [temp-ns (gensym)]
        (try
          (binding [*ns* (create-ns temp-ns)]
            (clojure.core/refer-clojure)
            (when (seq requirements)
              (apply require requirements))
            (load-string (slurp path)))
          (catch Exception e
            (println "Error loading config file" (str path))
            (print-cause-trace e)
            nil)
          (finally (remove-ns temp-ns)))))))


(defn load-configs
  "Initializes the configuration for the vault tool."
  [dir]
  (let [configure
        (fn [opts k & reqs]
          (assoc opts k
            (load-config-file
              (str dir \/ (name k) ".clj")
              reqs)))]
    (->
      {}
      (configure :blob-stores
        '(vault.blob.store
           [file :refer [file-store]]
           [memory :refer [memory-store]]))
      (configure :indexers))))
