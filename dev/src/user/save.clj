(ns user.save
  "This namespace will not be reloaded by the tools.namespace refresh code. The
  included protocol and functions offer a way to store in-memory state in a safe
  location while reloading the rest of the codebase."
  (:require
    [clojure.tools.namespace.repl :refer [disable-reload!]]))


(disable-reload!)



;; ## State Protocol

(defprotocol Memorable
  "Simple protocol to describe components which primarily use an in-memory
  state. This can be used to persist such components across code reloads."

  (snapshot
    [this]
    "Return a value representing the current state of the component. A nil
    value is interpreted as not needing storage.")

  (restore!
    [this state]
    "Restore the component's state from a value produced by an earlier
    snapshot. This is expected to be side-effecting; it does *NOT* replace the
    component value in the system map."))


;; By default, components don't save state and are not affected by restoration.
(extend-type Object
  Memorable

  (snapshot
    [this]
    nil)

  (restore!
    [this]
    this))



;; ## State Storage

(def storage
  "Temporary persistent storage which will not be destroyed during a reload."
  nil)


(defn save-states!
  "Takes a map of components and attempts to snapshot the state of each one.
  State values are stored in the `storage` var under the corresponding keys."
  [system]
  (let [states (->> system
                    (map (juxt key (comp snapshot val)))
                    (remove (comp nil? val))
                    (into {}))]
    (alter-var-root #'storage (constantly states))))


(defn restore-states!
  "Takes a map of components and attempts to restore each one's corresponding
  state from the `storage` var."
  [system]
  (doseq [[k state] storage]
    (when-let [component (get system k)]
      (save/restore! component state))))
