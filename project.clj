(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :dependencies
  [[byte-streams "0.1.10"]
   [clj-time "0.7.0"]
   [environ "0.5.0"]
   [potemkin "0.3.4"]
   [mvxcvi/clj-pgp "0.5.2"]
   [mvxcvi/puget "0.5.1"]
   [org.clojure/clojure "1.6.0"]
   [org.clojure/data.codec "0.1.0"]
   [prismatic/schema "0.2.2"]]

  :hiera
  {:cluster-depth 2
   :ignore-ns #{clojure potemkin}}

  :profiles
  {:coverage
   {:plugins
    [[lein-cloverage "1.0.2"]]}

   :tool
   {:dependencies
    [[mvxcvi/directive "0.4.2"]
     [org.clojure/tools.namespace "0.2.4"]]
    :jvm-opts []
    :repl-options {:init-ns vault.system}
    :source-paths ["tool"]}

   :repl [:tool]})
