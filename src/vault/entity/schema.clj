(ns vault.entity.schema
  (:require
    [schema.core :as schema]
    [vault.data.core :as data])
  (:import
    org.joda.time.DateTime
    vault.blob.digest.HashID))


;;;;; ENTITY SCHEMAS ;;;;;

(def ^:const root-type   :vault.entity/root)
(def ^:const update-type :vault.entity/update)


(def DatomOperation
  "Schema for an operation on a datom."
  (schema/enum :attr/set :attr/add :attr/del))


(def DatomFragment
  "Schema for a fragment of a datom. Basically, a partial datom vector with
  :op, :attr, and :value."
  [(schema/one DatomOperation "operation")
   (schema/one schema/Keyword "attribute")
   (schema/one schema/Any "value")])


(def DatomFragments
  "Schema for a vector of one or more datom fragments."
  [(schema/one DatomFragment "datoms")
   DatomFragment])


(def DatomUpdates
  "Schema for a map from entity ids to vectors of datom fragments."
  {HashID DatomFragments})


(def EntityRoot
  "Schema for an entity root value."
  {data/type-key (schema/eq root-type)
   :id String
   :owner HashID
   :time DateTime
   (schema/optional-key :data) DatomFragments})


(def EntityUpdate
  "Schema for an entity update value."
  {data/type-key (schema/eq update-type)
   :time DateTime
   :data DatomUpdates})



;;;;; PREDICATES ;;;;;

(defn root?
  "Determines whether the given value is an entity root."
  [value]
  (= root-type (data/type value)))


(defn update?
  "Determines whether the given value is an entity update."
  [value]
  (= update-type (data/type value)))
