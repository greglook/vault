(ns vault.entity.datom
  "Functions for handling atomic data assertions."
  (:require
    [vault.data.struct :as struct]
    [vault.entity.schema :as s]))


; TODO: make this deftype instead, implement equality checks and such?
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
        record (struct/data-value blob)]
    (condp = (struct/data-type blob)
      s/root-type
      (map-datoms (:time record) (:id blob) (:data record))
      s/update-type
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
