(ns vault.tool.main
  (:require
    [mvxcvi.directive :refer [command execute]]
    [puget.printer :refer [pprint cprint]]
    [vault.system :as sys]
    (vault.tool
      [blob :as blob-tool]
      [data :as data-tool]))
  #_ (:gen-class :main true))


;; UTILITY ACTIONS

(defn- debug-command
  [opts args]
  (cprint opts)
  (cprint args))


(defn- not-yet-implemented
  [opts args]
  (binding [*out* *err*]
    (println "This command is not yet implemented")))



;; COMMAND STRUCTURE

(def commands
  (command "vault [global opts] <command> [command args]"
    "Command-line tool for the vault data store."

    ["-v" "--verbose" "Show extra debugging messages."]
    ["-h" "--help"    "Show usage information."]

    (init [opts]
      (assoc opts :blob-store sys/blobs))


    (command "blob <action> [args]"
      "Low-level commands dealing with data blobs."

      (command "list [opts]"
        "Enumerate the stored blobs."

        ["-a" "--after" "Start enumerating blobs lexically following the given string."]
        ["-n" "--limit" "Limit the number of results returned." :parse-fn #(Integer/parseInt %)]

        (action blob-tool/list-blobs))

      (command "stat <hash-id> [hash-id ...]"
        "Show information about a stored blob."

        [nil "--pretty" "Format the info over multiple lines for easier viewing."
         :default true]

        (action blob-tool/stat-blob))

      (command "get <hash-id>"
        "Print the contents of a blob to stdout."
        (action blob-tool/get-blob))

      (command "put <source>"
        "Store a blob of data and print the resulting hash-id. If source is '-',
              data will be read from stdin. Otherwise, it should be a file to read
              content from."
        (action blob-tool/put-blob)))


    (command "data <action> [args]"
      "Interact with object entities and data."

      (command "show <hash-id> [hash-id ...]"
        "Inspect the contents of the given blobs, pretty-printing EDN values and
              showing hex for binary blobs."

        ["-b" "--binary" "Print blobs as binary even if they appear to be textual."]

        (action data-tool/show-blob))

      (command "create [args]"
        "Create a new object."

        ["-t" "--time" "Set the time to create the object root with. Defaults to the current time."]
        ["-i" "--id" "Set an identity for the object root. Defaults to a random string."]
        ["-a" "--attribute" "Provide an initial set of attributes for the object."]

        (action not-yet-implemented))

      (command "update <entity> <type> [args]"
        "Apply an update to an existing object."

        (action not-yet-implemented)))))


(defn -main [& args]
  (execute commands args))
