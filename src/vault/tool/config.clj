(ns vault.tool.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [puget.printer :refer [cprint]]
            (vault.store
              [file :refer [file-store]]
              [memory :refer [memory-store]])))


;; CONFIGURATION FILE LOADING

(def default-path
  "Location of Vault configuration files."
  (str (get (System/getenv) "HOME") "/.config/vault"))


(def ^:private config-readers
  "Map of EDN readers for supported types in config files."
  {'vault/file-store (partial apply file-store)
   'vault/memory-store (partial apply memory-store)})


(defn- find-configs
  "Locates a sequence of configuration files with a particular name pattern.
  For example, if `dir` is '/foo/bar/' and `k` is :baz, then file
  '/foo/bar/baz.edn' comes first (if it exists), followed by the files in
  '/foo/bar/baz/' (if it exists) in lexical order."
  [dir k]
  (let [config-file (io/file dir (str (name k) ".edn"))
        config-dir  (io/file dir (str (name k)))]
    (filter
      identity
      (cons (when (.exists config-file) config-file)
            (when (.exists config-dir) (sort (.listFiles config-dir)))))))


(defn- read-config
  "Reads an EDN configuration file."
  [path]
  (try
    (edn/read-string
      (slurp path)
      {:readers config-readers})
    (catch Exception e
      (println "Error loading config file" (str path))
      (.printStackTrace e)
      nil)))


(defn- load-configs
  "Loads a set of configuration files to form a merged config value. The final
  merged value is then assigned to the key `k` in the `opts` map. If the map
  already contains a value for that key, that value is merged _into the merged
  config value_ so that command-line options take precedence."
  [dir opts k]
  (let [config (->> (find-configs dir k)
                    (map read-config)
                    (apply merge))
        config (merge config (opts k))]
    (assoc opts k config)))


(defn initialize
  "Initializes the configuration for the vault tool."
  [opts]
  (let [config-dir (:config opts)
        types [:blob-stores :keys :indexers]]
    (reduce (partial load-configs config-dir) opts types)))



;; BLOB STORAGE CONFIGURATION

; Blob store configuration maps look like this:
;{:default :local, :local #vault/file-store (:sha256 "/home/$USER/var/vault")}


(defn- select-blob-store
  "Recursively selects a blob store in a config map."
  [stores selection]
  (->> (or selection :default)
       (iterate stores)
       (drop-while keyword?)
       first))


(defn setup-blob-store
  [opts]
  (let [stores (:blob-stores opts)]
    (when-not stores
      (println "No blob-stores configured!")
      (System/exit 1))
    (let [store (select-blob-store stores (:store opts))]
      (when-not store
        (println "No blob-store selected!")
        (System/exit 1))
      (assoc opts :store store))))


(defn list-blob-stores
  [opts args]
  (println "Available blob stores:")
  (let [blob-stores (:blob-stores opts)
        default     (:default blob-stores)]
    (doseq [[nickname store] blob-stores]
      (when-not (= :default nickname)
        (printf " %8s%s  " (name nickname) (if (= nickname default) \* \space))
        (cprint store {:width 180})))))
