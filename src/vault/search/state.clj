(ns vault.search.state
  "Entity state data search functions and index definitions."
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


;; ## Transaction Log

(def tx-types
  "Set of allowable transaction value types."
  (set [tx/root-type tx/update-type]))


(def tx-schema
  "Schema for a transaction log record."
  {:id    HashID       ; transaction blob hash-id
   :type  Keyword      ; transaction type (root/update)
   :time  DateTime     ; time of modification
   :owner HashID})     ; owner's public-key hash-id


(defn blob->tx
  "Projects a blob into a transaction record."
  [blob]
  ; TODO: parse blob?
  (let [tx (struct/data-value blob)
        type (struct/data-type blob)]
    (when (contains? tx-types type)
      {:tx (:id blob)
       :type type
       :time (:time tx)
       :owner (:owner tx)})))


; tx log range?



;; ## Datom Indexes

(def datom-schema
  "Schema for a datom record."
  {:op        Keyword     ; datom operation (:attr/set, :attr/add, etc)
   :entity    HashID      ; entity root hash-id
   :attribute Keyword     ; attribute keyword
   :value     schema/Any  ; serialized EDN value
   :tx        HashID      ; root or update blob hash-id
   :time      DateTime})  ; assertion timestamp from blob
