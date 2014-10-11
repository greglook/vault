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
      (let [store (some->> sys/core :defaults :store (get sys/core))]
        (assoc opts :blob-store store)))


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

      ; TODO: command to store EDN as a data blob
      )))


(defn -main [& args]
  (execute commands args))
