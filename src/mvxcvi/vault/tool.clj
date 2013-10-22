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



;; COMMAND TREE DEFINITION

; command nodes look like this:
#_
{:name "vault"
 :usage "[global opts] <command> [command args]"
 :desc "Command-line tool for the vault data store."
 :specs [["--blob-store" "Path to blob-store configuration." ...]]
 :init (fn [opts] ...)
 :action (fn [opts args] ...)
 :commands [{:name "blob"} ...]}


(defn- make-element-fn
  "Resolves an element as a function body or symbolic reference."
  [[element & body]]
  (when element
    (if (symbol? (first body))
      (first body)
      `(fn ~@body))))


(defn- make-command
  "Constructs a command node map from the given elements."
  [cmd-name usage desc specs init-element action-element command-elements]
  (let [cond-assoc (fn [coll [k v]] (if (and k v) (assoc coll k v) coll))
        nonempty-vec (fn [xs] (and (seq xs) (vec xs)))]
    (reduce cond-assoc
            {:name cmd-name
             :usage usage
             :desc desc}
            [[:specs    (nonempty-vec specs)]
             [:init     (make-element-fn init-element)]
             [:action   (make-element-fn action-element)]
             [:commands (nonempty-vec command-elements)]])))


(defmacro ^:private command
  "Macro to simplify building readable command trees."
  [usage desc & more]
  (let [[cmd-name usage] (string/split usage #" " 2)
        [specs more]     (split-with vector? more)
        elements         (group-by first (filter list? more))
        init-elements    (elements 'init)
        action-elements  (elements 'action)
        command-elements (elements 'command)]
    (when-not (every? list? more)
      (throw (IllegalArgumentException.
               (str "Non-list elements in '" cmd-name "' command definition: "
                    (filter (complement list?) more)))))
    (when (> (count init-elements) 1)
      (throw (IllegalArgumentException.
               (str "Multiple `init` elements in '" cmd-name
                    "' command definition: " init-elements))))
    (when (> (count action-elements) 1)
      (throw (IllegalArgumentException.
               (str "Multiple `action` elements in '" cmd-name
                    "' command definition: " action-elements))))
    (when (and command-elements action-elements)
      (throw (IllegalArgumentException.
               (str "Both `command` and `action` elements in '" cmd-name
                    "' command definition: " command-elements action-elements))))
    (make-command cmd-name usage desc specs
                  (first init-elements)
                  (first action-elements)
                  command-elements)))



;; COMMAND TREE EXECUTION

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
