(ns mvxcvi.vault.tool
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [cli]])
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



;; COMMAND TREE STRUCTURE

; command nodes look like this:
#_
{:name "vault"
 :usage "vault [global opts] <command> [command args]"
 :desc "Command-line tool for the vault data store."
 :specs [["--blob-store" "Path to blob-store configuration." ...]]
 :init (fn [opts] ...)
 :commands {"blob" {:name "blob"}}
 :action (fn [opts args] ...)}


(def ^:private ^:dynamic *command-branch*
  "Vector containing the command names on the current branch."
  [])


(defmacro ^:private command
  "Simple macro for building readable command trees."
  [usage desc & more]
  (let [[cname usage] (string/split usage #" " 2)
        [specs more] (split-with vector? more)
        command {:name cname
                 :usage `(string/join " " (concat *command-branch* [~usage]))
                 :desc desc
                 :specs (vec specs)}
        elements (group-by first (filter list? more))]
    (when (> (count (elements 'init)) 1)
      (throw (IllegalArgumentException.
               (str "Multiple `init` elements in command definition: " (elements 'init)))))
    (when (> (count (elements 'action)) 1)
      (throw (IllegalArgumentException.
               (str "Multiple `action` elements in command definition: " (elements 'action)))))
    (when (and (elements 'command) (elements 'action))
      (throw (IllegalArgumentException.
               (str "Command definition contains both `command` and `action` elements: " more))))
    (let [assoc-fn (fn [cmd [k & body]]
                     (if k
                       (if (symbol? (first body))
                         (assoc cmd (keyword k) (first body))
                         (assoc cmd (keyword k) `(fn ~@body)))
                       cmd))
          command (reduce assoc-fn command
                          [(first (elements 'init))
                           (first (elements 'action))])]
      (if-let [subcommands (elements 'command)]
        `(binding [*command-branch* (conj *command-branch* ~(:name command))]
           (->>
             ~(vec subcommands)
             (map (juxt :name identity))
             (into {})
             (assoc ~command :commands)))
        `(binding [*command-branch* (conj *command-branch* ~(:name command))]
           ~command)))))


(defn- handle-command
  "Handles a sequence of arguments following a command tree structure."
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
            (:action command) ((:action command) @new-opts command-args)
            :else (do (println banner) (System/exit 1))))))



;; COMMAND LINE INTERFACE

(def command-tree
  (command "vault [global opts] <command> [command args]"
    "Command-line tool for the vault data store."

    ["--blob-store" "Path to blob-store configuration."
     :default "~/.config/vault/blob-store.edn"]
    ["-v" "--verbose" "Show extra debugging messages."
     :flag true :default false]
    ["-h" "--help" "Show usage information."
     :flag true :default false]

    #_ (init [opts]
      (-> opts
          (assoc :blob-store (slurp (:blob-store opts)))))

    (command "blob [opts] <action> [args]"
      "Blob storage command."

      (command "list [opts]"
        "Enumerate the stored blobs."
        ; ... filtering/range options
        (action vector #_ blob-tool/list-blobs))

      (command "info <blobref>"
        "Show information about a stored blob."
        (action vector #_ blob-tool/blob-info))

      (command "get <blobref>"
        "Print the contents of a blob to stdout."
        (action vector #_ blob-tool/get-blob))

      (command "put"
        "Store a blob of data read from stdin and print the resulting blobref."
        (action vector #_ blob-tool/put-blob)))))


(defn -main [& args]
  (handle-command command-tree {} args))
