(ns mvxcvi.vault.tool
  (:require [clojure.tools.cli :refer [cli]])
  (:gen-class :main true))

;; Vault commands need to know:
;; blob-store configuration
;; indexer configuration
;; gpg identity


;; Ideas for tool commands:
;; $ vault --blob-store ~/.config/vault/blob-store.edn
;; $ vault blob list [start-blobref]
;; $ vault blob info <blobref>
;; $ vault blob get <blobref> > ./foo.dat
;; $ vault blob put < ./foo.dat > ./blobref.txt
;; $ vault object ...
;; $ vault group ...

;; So what's the algorithm?
;; - scan arguments for first occurrance of subcommand key
;;   NOTE: this means that an argument to an option CANNOT be any subcommand name!
;;   e.g. $ vault --some-option blob --another-option foo group show ...
;;        $ vault [--some-option] [blob --another-option foo ...]
;;
;; - run cli with the specs for this node on the pre-command args
;; - if the subcommand is 'help', change it to :help true in the opts map and recur
;; - if a subcommand was chosen, recurse with the subcommand node, the subcommand args, and the merged option map
;; - if no subcommand was chosen and :help is set, print the help message for this node
;; - if no subcommand was chosen and the node has an :action, execute the action with the opts and remaining args
;; - otherwise, error.




; command node:
#_
{:usage "vault [global opts] <command> [command args]"
 :desc "Command-line tool for the vault data store."
 :specs [["--blob-store" "Path to blob-store configuration." ...]]
 :init (fn [opts] ...)
 :commands {"blob" {:name "blob"}}
 :action (fn [opts args] ...)}


(defn handle-command
  [command opts args]
  (let [subcommand-names (into #{} (cons "help" (keys (:commands command))))

        [command-args [subcommand & subcommand-args]]
        (split-with (complement subcommand-names) args)

        [command-opts remaining-args banner]
        (apply cli command-args
               (str "Usage: " (:usage command) "\n\n" (:desc command))
               (:specs command))

        new-opts (merge opts command-opts)
        new-opts (delay (if-let [init (:init command)] (init new-opts) new-opts))]
    (if subcommand
      (if (= "help" subcommand)
        (recur command
               (assoc opts :help true)
               (concat command-args subcommand-args))
        (recur (get-in command [:commands subcommand])
               @new-opts
               subcommand-args)) ; NOTE: ignores unknown args before the subcommand
      (cond (:help opts) (do (println banner) (System/exit 0))
            (:action command) ((:action command) opts command-args)
            :else (do (println banner) (System/exit 1))))))


#_
(command "vault [global opts] <command> [command args]"
  "Command-line tool for the vault data store."

  ["--blob-store" "Path to blob-store configuration."
   :default "~/.config/vault/blob-store.edn"]
  ["-v" "--verbose" "Show extra debugging messages."
   :flag true :default false]
  ["-h" "--help" "Show usage information."
   :flag true :default false]

  (init [opts]
    (-> opts
        (assoc :blob-store (slurp (:blob-store opts)))))

  (command "blob [opts] <action> [args]"
    "Blob storage command."

    (command "list [opts]"
      "Enumerate the stored blobs."
      ; ... filtering/range options
      (action blob-tool/list-blobs))

    (command "info <blobref>"
      "Show information about a stored blob."
      (action blob-tool/blob-info))

    (command "get <blobref>"
      "Print the contents of a blob to stdout."
      (action blob-tool/get-blob))

    (command "put"
      "Store a blob of data and print the resulting blobref."
      (action blob-tool/put-blob))))



;; COMMAND LINE INTERFACE

(defn -main [& args]
  (let [[opts args banner]
        (cli
          args
          "Usage: vault [global opts] <system> <command> [command args]"
          ["--blob-store" "Path to blob-store configuration."
           :default "~/.config/vault/blob-store.edn"]
          ["-v" "--verbose" "Show extra debugging messages."
           :flag true :default false]
          ["-h" "--help"    "Show usage information."
           :flag true :default false])]
    (when (:help opts)
      (println banner)
      (System/exit 0))
    ;; TODO: command-line processing
    (println "Opts: " opts)
    (let [[system action & args] args]
      (println "System: " system)
      (println "Action: " action)
      (println "Args: " args))))
