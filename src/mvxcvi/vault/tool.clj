(ns mvxcvi.vault.tool
  (:require (clojure [edn :as edn]
                     [pprint :refer [pprint]])
            [mvxcvi.util.cli :refer [command execute]]
            (mvxcvi.vault.blob.store [file :refer [file-store]])
            (mvxcvi.vault.tool [blob :as blob-tool]))
  (:import java.io.FileNotFoundException)
  (:gen-class :main true))


;; Vault commands need to know:
;; blob-store configuration
;; indexer configuration
;; gpg identity

;; Ideas for tool commands:
;; $ vault object ...
;; $ vault group ...

(def config-paths
  "Location of Vault configuration files."
  {:blob-stores (str (get (System/getenv) "HOME") "/.config/vault/blob-stores.clj")})


(defn- initialize-blob-stores
  [opts]
  (try
    (let [config (slurp (:blob-stores config-paths))
          edn-readers {'vault/file-store (partial apply file-store)}
          stores (edn/read-string {:readers edn-readers} config)]
      (assoc opts :blob-stores stores))
    (catch java.io.FileNotFoundException e
      (assoc opts :blob-stores {}))))


(def commands
  (command "vault [global opts] <command> [command args]"
    "Command-line tool for the vault data store."

    ["-v" "--verbose" "Show extra debugging messages."
     :flag true :default false]
    ["-h" "--help" "Show usage information."
     :flag true :default false]

    (init initialize-blob-stores)

    (command "config <type>"
      "Show configuration information."

      (command "stores"
        "List the available blob stores."
        (action [opts args]
          (println "Available blob stores:")
          (doseq [[nickname store] (:blob-stores opts)]
            (when-not (= :default nickname)
              (printf " %8s   " (name nickname))
              (pprint store))))))

    (command "blob <action> [args]"
      "Blob storage command."

      (command "list [opts]"
        "Enumerate the stored blobs."
        ; ... filtering/range options
        (action blob-tool/list-blobs))

      (command "info <blobref>"
        "Show information about a stored blob."
        (action blob-tool/blob-info))

      (command "get <blobref> > blob.dat"
        "Print the contents of a blob to stdout."
        (action blob-tool/get-blob))

      (command "put < blob.dat"
        "Store a blob of data read from stdin and print the resulting blobref."
        (action blob-tool/put-blob)))))


(defn -main [& args]
  (execute commands args))
