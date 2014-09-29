(ns vault.entity.schema
  "Schema definitions for entity-related data, including datoms and transaction
  blobs."
  (:require
    [clj-time.core]
    [schema.core :as schema]
    [vault.blob.content]
    [vault.data.edn :as edn])
  (:import
    org.joda.time.DateTime
    vault.blob.content.HashID))


(def ^:const root-type   :vault.entity/root)
(def ^:const update-type :vault.entity/update)


(def DatomOperation
  "Schema for an operation on a datom."
  (schema/enum :attr/set :attr/add :attr/del))


(def DatomFragment
  "Schema for a fragment of a datom. Formed by a partial datom vector with
  `op`, `attribute`, and `value`."
  [(schema/one DatomOperation "operation")
   (schema/one schema/Keyword "attribute")
   (schema/one schema/Any "value")])


(def DatomFragments
  "Schema for a vector of one or more datom fragments."
  [(schema/one DatomFragment "datoms")
   DatomFragment])


(def DatomUpdates
  "Schema for a map from entity hash-ids to vectors of datom fragments."
  {HashID DatomFragments})


(def EntityRoot
  "Schema for an entity root value."
  {edn/type-key (schema/eq root-type)
   :id String
   :owner HashID
   :time DateTime
   (schema/optional-key :data) DatomFragments})


(def EntityUpdate
  "Schema for an entity update value."
  {edn/type-key (schema/eq update-type)
   :time DateTime
   :data DatomUpdates})
