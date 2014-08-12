(ns vault.entity.datom
  (:require
    [clj-time.core :as time]
    [clojure.set :as set]
    [clojure.string :as str]
    [schema.core :as schema]
    [vault.blob.core :as blob]
    [vault.data.core :as data]
    [vault.entity.tx :as tx]))


; TODO: make this deftype instead, implement equality checks and such.
(defrecord Datom [op entity attribute value tx time])


(defn datom
  "Constructs a new datom."
  [o e a v tx t]
  (->Datom o e a v tx t))


(defn tx-datoms
  "Reads a sequence of datoms from a transaction blob."
  [blob]
  (let [map-datoms
        (fn [time entity fragments]
          (map
            (fn [[op attr value]]
              (Datom. op entity attr value (:id blob) time))
            fragments))
        record (data/blob-value blob)]
    (condp = (:data/type blob)
      tx/root-type
      (map-datoms (:time record) (:id blob) (:data record))
      tx/update-type
      (mapcat (partial apply map-datoms (:time record)) (:data record)))))


(defn apply-datom
  "Applies a datom to an entity state map to produce an updated state value."
  [entity {:keys [op attribute value]}]
  (let [current (get entity attribute)]
    (case op
      :attr/set
      (assoc entity attribute value)

      :attr/add
      (assoc entity attribute
        (cond
          (set? current)
          (conj current value)

          (nil? current)
          (sorted-set value)

          :else
          (sorted-set current value)))

      :attr/del
      (cond
        (nil? value)
        (dissoc entity attribute)

        (set? current)
        (let [new-set (disj current value)]
          (if (empty? new-set)
            (dissoc entity attribute)
            (assoc entity attribute new-set)))

        (= current value)
        (dissoc entity attribute)

        :else
        entity))))


(defn entity-state
  "Given a sequence of datoms, return a map giving the 'current' state of some
  entity."
  [root-id datoms]
  (reduce
    apply-datom
    {:vault.entity/id root-id}
    datoms))
