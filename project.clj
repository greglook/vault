(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :aliases
  {"tool-jar"
   ["with-profile" "tool" "uberjar"]}

  :dependencies
  [[byte-streams "0.1.10"]
   [clj-time "0.7.0"]
   [potemkin "0.3.4"]
   [mvxcvi/clj-pgp "0.5.0"]
   [mvxcvi/puget "0.5.1"]
   [org.clojure/clojure "1.6.0"]
   [org.clojure/data.codec "0.1.0"]
   [org.clojure/tools.cli "0.2.4"]]

  :hiera
  {:cluster-depth 2
   :ignore-ns #{potemkin}}

  :profiles
  {:coverage
   {:plugins
    [[lein-cloverage "1.0.2"]]}

   :tool
   {:dependencies
    [[mvxcvi/directive "0.1.0"]]
    :source-paths ["tool"]
    :jar-name "vault-tool-%s.jar"
    :uberjar-name "vault-tool.jar"
    :main vault.tool.main
    :aot :all}

   :repl
   {:dependencies
    [[mvxcvi/directive "0.1.0"]
     [org.clojure/tools.namespace "0.2.4"]]
    :source-paths ["repl" "tool"]}})
