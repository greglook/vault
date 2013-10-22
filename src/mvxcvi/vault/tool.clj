(ns mvxcvi.vault.tool
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [cli]]
            (mvxcvi.vault.tool [blob :as blob-tool]))
  (:gen-class :main true))


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

(defn- usage-banner
  "Builds a usage banner given a command map."
  [branch cmd]
  (let [{:keys [usage desc commands]} cmd]
    (str "Usage: " (string/join " " branch) " " usage "\n\n" desc
         (when (seq commands)
           (str "\n\n Subcommand   Description\n"
                    " ----------   -----------\n"
                (->> commands
                     (map #(format " %10s   %s" (:name %) (:desc %)))
                     (string/join "\n")))))))


(defn- handle-command
  "Handles a sequence of arguments following a command tree structure."
  ([cmd args]
   (handle-command cmd [] {} args))

  ([cmd branch opts args]
   (if (some #{"help"} args)
     ; Recur with :help set in opts and the "help" argument removed.
     (recur cmd branch
            (assoc opts :help true)
            (filter #(not= "help" %) args))

     (let [branch (conj branch (:name cmd))
           subcmds (:commands cmd)
           subcmd-names (apply hash-set (map :name subcmds))
           usage (usage-banner branch cmd)

           ; Test for a subcommand invocation in the argument list.
           [cmd-args [subcmd & subcmd-args]]
           (split-with (complement subcmd-names) args)

           ; Parse arguments with command specs, if present.
           [cmd-opts action-args banner]
           (if-let [specs (:specs cmd)]
             (apply cli cmd-args usage (:specs cmd))
             [opts cmd-args usage])

           ; Merge parsed opts and initialize them.
           opts (merge opts cmd-opts (when (:help opts) {:help true}))
           opts ((or (:init cmd) identity) opts)]

       ;(clojure.pprint/pprint {:branch branch, :opts opts, :action-args action-args, :subcmd subcmd, :subcmd-args subcmd-args})

       (if-let [subcmd (some #(and (= (:name %) subcmd) %) subcmds)]
         ; Recur on selected subcommand.
         (if-not (empty? action-args)
           (throw (IllegalArgumentException.
                    (str "Unparsed arguments before command: " action-args)))
           (recur subcmd branch opts subcmd-args))

         ; Act on given command, either to print help or execute the action.
         (cond (:help opts)
               (do (println banner)
                   (System/exit 0))

               (:action cmd)
               ((:action cmd) opts action-args)

               :else
               (do (when-not (empty? action-args)
                     (println "Unrecognized arguments:"
                              (string/join " " action-args) "\n"))
                   (println banner)
                   (System/exit 1))))))))



;; COMMAND LINE INTERFACE

;; Vault commands need to know:
;; blob-store configuration
;; indexer configuration
;; gpg identity


;; Ideas for tool commands:
;; $ vault blob list [prefix]
;; $ vault blob info <blobref>
;; $ vault blob get <blobref> > ./foo.dat
;; $ vault blob put < ./foo.dat > ./blobref.txt
;; $ vault object ...
;; $ vault group ...

(def command-tree
  (command "vault [global opts] <command> [command args]"
    "Command-line tool for the vault data store."

    ["--blob-store" "Path to blob-store configuration."
     :default "~/.config/vault/blob-store.edn"]
    ["-v" "--verbose" "Show extra debugging messages."
     :flag true :default false]
    ["-h" "--help" "Show usage information."
     :flag true :default false]

    (init identity)

    (command "blob [opts] <action> [args]"
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
  (handle-command command-tree args))
