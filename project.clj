(defproject mvxcvi/vault "0.4.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :aliases {"docs" ["do" ["hiera"] ["doc"] ["marg" "--multi" "--dir" "target/doc/marginalia"]]
            "tests" ["do" ["check"] ["test"] ["cloverage"]]
            "tool-repl" ["with-profile" "+tool" "repl"]}

  :plugins [[codox "0.8.10"]
            [lein-cloverage "1.0.2"]
            [lein-marginalia "0.8.0"]]

  :dependencies [[byte-streams "0.1.13"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [clj-time "0.9.0"]
                 [compojure "1.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [environ "1.0.0"]
                 [mvxcvi/clj-pgp "0.5.4"]
                 [mvxcvi/puget "0.6.6"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [prismatic/schema "0.3.3"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring-middleware-format "0.4.0"]]

  :hiera {:path "target/doc/ns-hiera.png"
          :vertical? false
          :cluster-depth 2
          :ignore-ns #{user clojure byte-streams clj-time}}

  :codox {:defaults {:doc/format :markdown}
          :exclude #{user}
          :output-dir "target/doc/api"
          :src-dir-uri "https://github.com/greglook/vault/blob/develop/"
          :src-linenum-anchor-prefix "L"}

  :profiles {:repl {:source-paths ["dev/src"]
                    :dependencies [[org.clojure/tools.namespace "0.2.8"]]
                    :jvm-opts ["-DVAULT_LOG_APPENDER=repl"
                               "-DVAULT_LOG_LEVEL=DEBUG"]}

             :test {:resource-paths ["test-resources"]
                    :jvm-opts ["-DVAULT_LOG_APPENDER=nop"
                               "-DVAULT_LOG_LEVEL=TRACE"] }

             :tool {:source-paths ["tool"]
                    :dependencies [[mvxcvi/directive "0.4.2"]]}

             :uberjar {:aot :all
                       :target-path "target/uberjar"
                       :main vault.service.main}})
