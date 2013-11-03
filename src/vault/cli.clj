(ns vault.cli
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [cli]]))


;; COMMAND DEFINITION

; Command nodes look like this:
#_
{:name "cmd"
 :usage "[global opts] <command> [command args]"
 :desc "Command-line tool for ..."
 :specs [["--some-option" "Path to foo" ...]]
 :init (fn [opts] ...)
 :action (fn [opts args] ...)
 :commands [{:name ...} ...]}


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


(defmacro command
  "Macro to simplify building readable command trees.

  The result of this macro is a compile-time map value representing the declared
  tree of commands. This map can be used to interpret and take actions specified
  by a sequence of arguments using the `execute` function."
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



;; COMMAND EXECUTION

(defn- usage-banner
  [branch cmd]
  (let [{:keys [usage desc commands]} cmd]
    (str "Usage: " (string/join " " branch) " " usage "\n\n" desc
         (when (seq commands)
           (str "\n\n Subcommand   Description\n"
                    " ----------   -----------\n"
                (->> commands
                     (map #(format " %10s   %s" (:name %) (:desc %)))
                     (string/join "\n")))))))


(defn- parse-command-args
  [usage opts specs args]
  (if specs
    ; Parse arguments with command specs and merge opts.
    (let [[command-opts action-args banner]
         (apply cli args usage specs)
         opts (merge opts command-opts (when (:help opts) {:help true}))]
      [banner opts action-args])
    [usage opts args]))


(defn- parse-opts
  [usage cmd opts args]
  (let [subcommands (:commands cmd)
        subcommand-names (apply hash-set (map :name subcommands))
        ; Test for a subcommand invocation in the argument list.
        [command-args [subcommand & subcommand-args]]
        (split-with (complement subcommand-names) args)
        ; Parse command args with option specs.
        [banner opts action-args]
        (parse-command-args usage opts (:specs cmd) command-args)
        ; Map subcommand to actual command map.
        subcommand (and subcommand
                        (some #(and (= (:name %) subcommand) %)
                              subcommands))]
    [banner opts action-args subcommand subcommand-args]))


(defn- execute-action
  [usage action opts args]
  (cond (:help opts)
        (do (println usage)
            (System/exit 0))

        action
        (action opts args)

        :else
        (do (when-not (empty? args)
              (println "Unrecognized arguments:" (string/join " " args) "\n"))
            (println usage)
            (System/exit 1))))


(defn execute
  "Parses a sequence of arguments using a command map. The action function
  associated with a given leaf command will be called with a merged options map
  and any remaining arguments."
  ([cmd args]
   (execute cmd {} args))

  ([cmd opts args]
   (execute cmd [] opts args))

  ([cmd branch opts args]
   (if (some #{"help"} args)
     ; Recur with :help set in opts and the "help" argument removed.
     (recur cmd branch
            (assoc opts :help true)
            (filter #(not= "help" %) args))
     ; Parse arguments, update options and find subcommands.
     (let [branch (conj branch (:name cmd))
           usage (usage-banner branch cmd)
           [usage opts action-args subcommand subcommand-args]
           (parse-opts usage cmd opts args)
           opts ((or (:init cmd) identity) opts)]
       ;(clojure.pprint/pprint {:branch branch, :opts opts, :action-args action-args, :subcommand subcommand, :subcommand-args subcommand-args})
       (if subcommand
         ; Recur on selected subcommand.
         (if-not (empty? action-args)
           (throw (IllegalArgumentException.
                    (str "Unparsed arguments before command: " action-args)))
           (recur subcommand branch opts subcommand-args))
         ; Act on current command, either to print help or execute the action.
         (execute-action usage (:action cmd) opts action-args))))))
