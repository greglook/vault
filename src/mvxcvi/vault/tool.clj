(ns mvxcvi.vault.tool
  (:require [mvxcvi.util.cli :refer [command execute]]
            (mvxcvi.vault.tool [blob :as blob-tool]))
  (:gen-class :main true))


;; Vault commands need to know:
;; blob-store configuration
;; indexer configuration
;; gpg identity

;; Ideas for tool commands:
;; $ vault object ...
;; $ vault group ...


(def commands
  (command "vault [global opts] <command> [command args]"
    "Command-line tool for the vault data store."

    ["--blob-store" "Path to blob-store configuration."
     :default "~/.config/vault/blob-store.edn"]
    ["-v" "--verbose" "Show extra debugging messages."
     :flag true :default false]
    ["-h" "--help" "Show usage information."
     :flag true :default false]

    (init identity)

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
