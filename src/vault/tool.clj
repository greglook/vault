(ns vault.tool
  (:require (clojure
              [edn :as edn]
              [pprint :refer [pprint]])
            (vault.blob.store
              [file :refer [file-store]])
            [vault.cli :refer [command execute]]
            (vault.tool
              [blob :as blob-tool]))
  (:import java.io.FileNotFoundException)
  (:gen-class :main true))


;; Vault commands need to know:
;; blob-store configuration
;; indexer configuration
;; gpg identity


(def config-paths
  "Location of Vault configuration files."
  {:blob-stores (str (get (System/getenv) "HOME")
                     "/.config/vault/blob-stores.edn")})


(def ^:private config-readers
  "Map of EDN readers for supported types in config files."
  {'vault/file-store (partial apply file-store)})



;; BLOB STORAGE

(defn- select-blob-store
  [stores selection]
  (->> (or selection :default)
       (iterate stores)
       (drop-while keyword?)
       first))


(defn- initialize-blob-stores
  [opts]
  (try
    (let [config (slurp (:blob-stores config-paths))
          stores (edn/read-string {:readers config-readers} config)
          store (select-blob-store stores (:store opts :default))]
      (assoc opts :blob-stores stores :store store))
    (catch java.io.FileNotFoundException e
      (assoc opts :blob-stores {}))))


(defn- require-blob-store
  [opts]
  (when-not (:store opts)
    (println "No blob-store initialized.")
    (System/exit 1))
  opts)


(defn- list-blob-stores
  [opts args]
  (println "Available blob stores:")
  (let [blob-stores (:blob-stores opts)
        default     (:default blob-stores)]
    (doseq [[nickname store] blob-stores]
      (when-not (= :default nickname)
        (printf " %8s%s  " (name nickname) (if (= nickname default) \* \space))
        (pprint store)))))



;; COMMAND STRUCTURE

(def commands
  (command "vault [global opts] <command> [command args]"
    "Command-line tool for the vault data store."

    ["--store" "Select blob store to use."
     :parse-fn keyword]
    ["-v" "--verbose" "Show extra debugging messages."
     :flag true :default false]
    ["-h" "--help" "Show usage information."
     :flag true :default false]

    (init initialize-blob-stores)

    (command "config <type>"
      "Show configuration information."

      (command "stores"
        "List the available blob stores."
        (action list-blob-stores)))

    (command "blob <action> [args]"
      "Low-level commands dealing with data blobs."

      (init require-blob-store)

      (command "list [opts]"
        "Enumerate the stored blobs."

        ["-s" "--start" "Start enumerating blobs lexically following the start string."]
        ["-n" "--count" "Limit the number of results returned." :parse-fn #(Integer. %)]

        (action blob-tool/list-blobs))

      (command "stat <blobref>"
        "Show information about a stored blob."

        ["--pretty" "Format the info over multiple lines for easier viewing."
         :flag true :default true]

        (action blob-tool/blob-info))

      (command "get <blobref> > blob.dat"
        "Print the contents of a blob to stdout."
        (action blob-tool/get-blob))

      (command "put < blob.dat"
        "Store a blob of data read from stdin and print the resulting blobref."
        (action blob-tool/put-blob)))))


(defn -main [& args]
  (execute commands args)
  (shutdown-agents))
