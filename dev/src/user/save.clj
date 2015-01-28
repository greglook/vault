(ns user.save
  (:require
    [clojure.tools.namespace.repl :refer [disable-reload!]]))


(disable-reload!)


(def storage
  "Temporary persistent storage which will not be destroyed during a reload."
  nil)


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


(extend-type Object
  Memorable

  (snapshot
    [this]
    nil)

  (restore!
    [this]
    this))
