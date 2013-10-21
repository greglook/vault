(ns mvxcvi.vault.tool
  (:require [clojure.tools.cli :refer [cli]])
  (:gen-class :main true))

;; Vault commands need to know:
;; blob-store configuration
;; indexer configuration
;; gpg identity


;; Ideas for tool commands:
;; $ vault --blob-store ~/.config/vault/blob-store.edn
;; $ vault blob list
;; $ vault blob get sha256:123456789 > ./foo.dat
;; $ vault blob put < ./foo.dat > ./blobref.txt
;; $ vault object ...
;; $ vault group ...



;; COMMAND LINE INTERFACE

(defn -main [& args]
  (let [[opts args banner]
        (cli
          args
          "Usage: vault [global opts] <command> [command args]"
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
    (println "Args: " args)))
