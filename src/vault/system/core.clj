(ns vault.system.core
  "Functions for controlling the core Vault system's lifecycle."
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component])
  (:import
    java.io.File))


;; ## System Configuration

(def system
  "Vault system var."
  nil)


(defn- clj-file?
  "Determines whether the given file is a clojure file."
  [^File file]
  (and (.isFile file)
       (.matches (.getName file) ".*\\.clj$")))


(defn include
  "Load a config file or directory. If the path points to a directory, all
  files with names ending in `.clj` within it will be loaded recursively."
  [path]
  (let [file (io/file path)]
    (binding [*ns* (find-ns 'vault.system.config)]
      (if (.isDirectory file)
        (doseq [f (file-seq file)]
          (when (clj-file? f)
            (load-file (str f))))
        (load-file path)))))



;; ## Lifecycle

(defn init!
  "Initializes the Vault system with the given configuration."
  [& configs]
  (alter-var-root #'system (constantly (component/system-map)))
  (doseq [cfg configs]
    (include cfg)))


(defn start!
  "Starts the Vault system."
  []
  (log/info "Starting Vault system...")
  (alter-var-root #'system component/start)
  :started)


(defn stop!
  "Stops the Vault system."
  []
  (log/info "Stopping Vault system...")
  (alter-var-root #'system component/stop)
  :stopped)


(defn -main []
  (init!)
  (.addShutdownHook
    (Runtime/getRuntime)
    (Thread. ^Runnable stop! "Vault Shutdown Hook"))
  (start!)
  (log/info "System started, entering active mode..."))
