(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :dependencies
  [[byte-streams "0.1.13"]
   [clj-time "0.8.0"]
   [environ "1.0.0"]
   [potemkin "0.3.9"]
   [mvxcvi/clj-pgp "0.5.3"]
   [mvxcvi/puget "0.6.3"]
   [org.clojure/clojure "1.6.0"]
   [org.clojure/data.codec "0.1.0"]
   [prismatic/schema "0.2.6"]]

  :hiera
  {:cluster-depth 2
   :ignore-ns #{clojure byte-streams clj-time potemkin}}

  :profiles
  {:coverage
   {:plugins
    [[lein-cloverage "1.0.2"]]}

   :tool
   {:dependencies
    [[mvxcvi/directive "0.4.2"]
     [org.clojure/tools.namespace "0.2.7"]]
    :jvm-opts []
    :repl-options {:init-ns vault.system}
    :source-paths ["tool"]}

   :repl [:tool]})
