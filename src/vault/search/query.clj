(ns vault.search.query
  "Definitions for common index configurations and queries."
  (:require
    [clj-time.core :as time]
    [schema.core :as schema]
    [vault.blob.content]
    [vault.data.struct :as struct]
    (vault.entity
      [tx :as tx])
    [vault.search.index :as index])
  (:import
    clojure.lang.Keyword
    org.joda.time.DateTime
    vault.blob.content.HashID))


(defn- blob->tx
  "Projects a blob into a transaction record."
  [blob]
  ; TODO: parse blob?
  (when-let [tx (struct/data-value blob)]
    {:tx (:id blob)
     :type (struct/data-type blob)
     :time (:time tx)
     :owner (:owner tx)}))


(def tx-log
  "Stores a log of entity transactions."
  {:schema
   {:id    HashID       ; transaction blob hash-id
    :type  Keyword      ; transaction type (root/update)
    :time  DateTime     ; time of modification
    :owner HashID}      ; owner's public-key hash-id

   :unique-key :tx

   :query-types
   {:history [:owner :time]}

   :projection
   (index/filter-types
     blob->tx
     #{:vault.entity/root
       :vault.entity/update})})


(def entity-datoms
  "Stores datoms."
  {:schema
   {:op        Keyword     ; datom operation (:attr/set, :attr/add, etc)
    :entity    HashID      ; entity root hash-id
    :attribute Keyword     ; attribute keyword
    :value     schema/Any  ; serialized EDN value
    :tx        HashID      ; root or update blob hash-id
    :time      DateTime}   ; assertion timestamp from blob

   ; autogenerate the key
   :unique-key nil

   :query-types
   {:eavt [:entity :attribute :value :time]
    :aevt [:attribute :entity :value :time]
    :avet [:attribute :value :entity :time]  ; only 'indexed' attrs
    :vaet [:value :attribute :entity :time]} ; only vault/ref values

   :projection
   (index/filter-types
     tx/tx->datoms
     #{:vault.entity/root
       :vault.entity/update})})
