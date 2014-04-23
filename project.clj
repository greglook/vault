(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :dependencies
  [[byte-streams "0.1.6"]
   [mvxcvi/clj-pgp "0.1.0-SNAPSHOT"]
   [mvxcvi/directive "0.1.0"]
   [mvxcvi/puget "0.4.0-SNAPSHOT"]
   [org.clojure/clojure "1.5.1"]
   [org.clojure/data.codec "0.1.0"]
   [org.clojure/tools.cli "0.2.4"]]

  :profiles
  {:dev {:source-paths ["dev"]}}

  :hiera
  {:cluster-depth 2
   :ignore-ns #{byte-streams mvxcvi.directive vault.tool}})
