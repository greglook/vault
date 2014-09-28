(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :plugins
  [[codox "0.8.10"]]

  :dependencies
  [[byte-streams "0.1.13"]
   [clj-time "0.8.0"]
   [environ "1.0.0"]
   [mvxcvi/clj-pgp "0.5.4"]
   [mvxcvi/puget "0.6.4"]
   [org.clojure/clojure "1.6.0"]
   [org.clojure/data.codec "0.1.0"]
   [prismatic/schema "0.3.0"]]

  :codox
  {:output-dir "target/doc/codox"
   :src-dir-uri "https://github.com/greglook/vault/blob/develop/"
   :src-linenum-anchor-prefix "L"}

  :hiera
  {:path "target/doc/ns-hiera.png"
   :vertical? false
   :cluster-depth 2
   :ignore-ns #{clojure byte-streams clj-time}}

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
