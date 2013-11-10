(ns vault.tool
  (:require
    [puget.printer :refer [pprint cprint]]
    (vault.tool
      [command :refer [command execute]]
      [config :as config]
      [blob :as blob-tool]))
  (:gen-class :main true))


;; UTILITY ACTIONS

(defn- debug-command
  [opts args]
  (cprint opts)
  (cprint args))


(defn- not-yet-implemented
  [opts args]
  (println "This command is not yet implemented")
  (System/exit 1))



;; COMMAND STRUCTURE

(def commands
  (command "vault [global opts] <command> [command args]"
    "Command-line tool for the vault data store."

    ["--config" "Set path to vault configuration."
     :default config/default-path]
    ["--store" "Select blob store to use."
     :parse-fn keyword]
    ["-v" "--verbose" "Show extra debugging messages."
     :flag true :default false]
    ["-h" "--help" "Show usage information."
     :flag true :default false]

    (init config/initialize)

    (command "config <type>"
      "Show configuration information."

      (command "dump"
        "Prints out a raw version of the configuration map."

        ["--pretty" "Formats the info over multiple lines for easier viewing."
         :flag true :default false]

        (action [opts args]
          (if (:pretty opts)
            (cprint opts)
            (prn opts))))

      (command "stores"
        "List the available blob stores."
        (action config/list-blob-stores)))

    (command "blob <action> [args]"
      "Low-level commands dealing with data blobs."

      (init config/setup-blob-store)

      (command "list [opts]"
        "Enumerate the stored blobs."

        ["-s" "--start" "Start enumerating blobs lexically following the start string."]
        ["-n" "--count" "Limit the number of results returned." :parse-fn #(Integer/parseInt %)]

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
        (action blob-tool/put-blob)))

    (command "object <action> [args]"
      "Interact with object entities and data."

      (command "create [args]"
        "Create a new object."

        ["--time" "Set the time to create the object root with. Defaults to the current time."]
        ["--id" "Set an identity for the object root. Defaults to a random string."]
        ["--attributes" "Provide an initial set of attributes for the object."]
        ["--value" "Set the initial object value to the given reference."]

        (action not-yet-implemented))

      (command "update <object> <type> [args]"
        "Apply an update to an existing object."

        (action not-yet-implemented)))))


(defn -main [& args]
  (execute commands args)
  (shutdown-agents))
