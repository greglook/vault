(ns vault.entity.core
  (:require
    [vault.data.core :as data])
  (:import
    java.util.Date))


(defn entity
  "Constructs a new entity value."
  [owner]
  ; TODO: verify public key exists
  ; TODO: allow control of the id and time values.
  ; TODO: allow optional initial data.
  (data/typed-map
    :vault.entity/root
    :id "random-string"
    :time (Date.)
    :owner owner))


(defn update
  "Constructs a new update value."
  [data]
  ; TODO: verify that each of the data keys is an entity root.
  (data/typed-map
    :vault.entity/update
    :time (Date.)
    :data data))


(defn delete
  "Constructs a new delete marker value."
  [target]
  ; TODO: verify that target exists and is a root, update, or delete blob.
  (data/typed-map
    :vault.entity/delete
    :time (Date.)
    :target target))



;; DATOM FUNCTIONS

(defrecord Datom [op entity attribute value tx time])


(defn blob->datoms
  "Converts a blob into a sequence of datoms."
  [blob]
  (let [map-datoms
        (fn [time entity updates]
          (map
            (fn [[op attr value]]
              (Datom. op entity attr value (:id blob) time))
            updates))
        data (-> blob :data/values first :data)]
    (case (:data/type blob)
      :vault.entity/root
      (map-datoms (:time data) (:id blob) data)
      :vault.entity/update
      (mapcat (partial apply map-datoms (:time data)) data))))
