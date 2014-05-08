(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :aliases
  {"tool-jar"
   ["with-profile" "tool" "uberjar"]
   "trepl"
   ["with-profile" "repl" ["trampoline" "repl"]]}

  :dependencies
  [[byte-streams "0.1.10"]
   [potemkin "0.3.4"]
   [mvxcvi/clj-pgp "0.5.0"]
   [mvxcvi/puget "0.4.0-SNAPSHOT"]
   [org.clojure/clojure "1.5.1"]
   [org.clojure/data.codec "0.1.0"]
   [org.clojure/tools.cli "0.2.4"]]

  :hiera
  {:cluster-depth 2
   :ignore-ns #{potemkin}}

  :profiles
  {:tool
   {:dependencies
    [[mvxcvi/directive "0.1.0"]]
    :source-paths ["tool"]
    :jar-name "vault-tool-%s.jar"
    :uberjar-name "vault-tool.jar"
    :main vault.tool.main
    :aot :all}

   :repl
   {:dependencies
    [[org.clojure/tools.namespace "0.2.4"]]
    :source-paths ["repl"]}})
