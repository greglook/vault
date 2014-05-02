(ns vault.data.state
  (:require
    [vault.format.edn :as edn-data])
  (:import
    java.util.Date))


(defn entity
  "Constructs a new entity value."
  [owner]
  ; TODO: verify public key exists
  ; TODO: allow control of the id and time values.
  ; TODO: allow optional initial data.
  (assoc
    (edn-data/typed-map :vault.data/entity)
    :id "random-string"
    :time (Date.)
    :owner owner))


(defn update
  "Constructs a new update value."
  [data]
  ; TODO: verify that each of the data keys is an entity root.
  (assoc
    (edn-data/typed-map :vault.data/update)
    :time (Date.)
    :data data))


(defn delete
  "Constructs a new delete marker value."
  [target]
  ; TODO: verify that target exists and is an entity, update, or delete blob.
  (assoc
    (edn-data/typed-map :vault.data/delete)
    :time (Date.)
    :target target))
