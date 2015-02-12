(ns user
  (:require
    (clj-time
      [core :as time]
      [coerce :as coerce-time]
      [format :as format-time])
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.stacktrace :refer [print-cause-trace]]
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    (com.stuartsierra
      [component :as component]
      [dependency :as dependency])
    [environ.core :refer [env]]
    [puget.printer :as puget]
    [user.save :as save]
    (vault.blob
      [content :as content]
      [store :as store])
    [vault.entity.datom]
    [vault.system :as sys :refer [system init! start! stop!]]))


(puget.data/extend-tagged-map vault.blob.content.Blob 'vault.tool/blob)
(puget.data/extend-tagged-value
  vault.entity.datom.Datom 'vault.tool/datom
  (juxt :op :entity :attribute :value :tx :time))

; TODO: extend save/Memorable to in-memory blob and index storage components



;; ## System Lifecycle

(defn save!
  "Saves state to storage before a reload."
  []
  (save/save-states! system)
  :saved)


(defn load!
  "Loads state from storage after a reload. Should be called after `init!`."
  []
  (save/restore-states! system)
  :loaded)


(defn go!
  "Initializes with the default config and starts the system."
  []
  (sys/include (env :vault-config "dev/config/dev.clj"))
  (init!)
  (load!)
  (start!))


(defn reload!
  "Reloads all changed namespaces to update code, then re-launches the system."
  []
  (stop!)
  (save!)
  (refresh :after 'user/go!))



;; ## Utility Functions

(defn system-dependencies
  "Generates a map of the system dependencies - must be run after `init!` is
  called. Only the component keys in `ks` will be included as roots, or all
  components if no keys are provided."
  [& ks]
  (let [graph (component/dependency-graph
                system
                (or (seq ks) (keys system)))]
    (->>
      (dependency/nodes graph)
      (map #(vector % (dependency/immediate-dependencies graph %)))
      (into {}))))


(defn draw-dependency-graph!
  "Writes the system dependency graph to the given filename. A full sequence
  of commands to get a graph from this would be:

  - `$ lein repl`
  - `user=> (init!)`
  - `user=> (draw-dependency-graph! \"target/system.dot\")
  - `$ dot -Tsvg < target/system.dot > target/system.svg`"
  [f]
  (let [content
        (with-out-str
          (let [graph (system-dependencies)]
            (println "digraph go_ci_components {")
            (doseq [component (keys graph)]
              (printf "    %s;\n" (name component)))
            (doseq [[component links] graph
                    link links]
              (printf "    %s -> %s;\n" (name component) (name link)))
            (println "}")))]
    (if f
      (spit f content)
      (print content))))
