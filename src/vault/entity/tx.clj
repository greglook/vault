(ns vault.entity.tx
  "Entity data is stored in _transaction_ data blobs. These contain entity
  _roots_ and _updates_.

  A root blob creates a new entity by establishing a stable hash identifier as
  an _identity_. An update blob adds further modifications to one or more existing
  entities, identified by their root hash ids."
  (:require
    [clj-time.core :as time]
    [clojure.set :as set]
    [clojure.string :as str]
    [schema.core :as schema]
    ;[vault.blob.content]
    [vault.blob.store :as store]
    (vault.data
      [edn :as edn]
      [signature :as sig]
      [struct :as struct])
    (vault.entity
      [datom :as datom]))
  (:import
    org.joda.time.DateTime
    vault.blob.content.HashID))


;; ## Schemas

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



;; ## Entity Roots

(defn root?
  "Determines whether the given value is an entity root."
  [value]
  (= root-type (edn/value-type value)))


(defn- random-id!
  "Generates a hexadecimal identifier string from a sequence of random bytes."
  []
  (let [buf (byte-array 16)]
    (.nextBytes (java.security.SecureRandom.) buf)
    (.toString (BigInteger. 1 buf) 16)))


(defn root-value
  "Constructs a new entity root value."
  [{:keys [owner id time data]}]
  (when-not owner
    (throw (IllegalArgumentException. "Cannot create entity without owner")))
  (when data
    (schema/validate DatomFragments data))
  (cond->
    (edn/typed-map
      root-type
      :id (or id (random-id!))
      :time (or time (time/now))
      :owner owner)
    data (assoc :data data)))


(defn root->blob
  "Constructs a new entity blob for the given owner."
  [store sig-provider args]
  (sig/sign-value
    (root-value args)
    store
    sig-provider
    (:owner args)))


(defn validate-root
  "Checks the structure and signatures on an entity root blob. Returns a blob
  record with verified signatures."
  [blob store]
  (schema/validate EntityRoot (struct/data-value blob))
  (let [blob (sig/verify-sigs blob store)
        sigs (:data/signatures blob)
        owner (:owner (struct/data-value blob))]
    (when-not (contains? sigs owner)
      (throw (IllegalStateException.
               (str "Entity blob blob " (:id blob)
                    " does not have a valid signature by the owning key "
                    owner))))
    blob))



;; ## Entity Updates

(defn update?
  "Determines whether the given value is an entity update."
  [value]
  (= update-type (edn/value-type value)))


(defn- get-owner
  "Looks up the owner for the given entity root id. Throws an exception if any
  of the ids is not an entity root."
  [store root-id]
  (let [blob (edn/parse-data (store/get store root-id))]
    (when-not blob
      (throw (IllegalArgumentException.
               (str "Cannot get owner for nonexistent entity " root-id))))
    (when-not (root? (struct/data-value blob))
      (throw (IllegalArgumentException.
               (str "Cannot get owner for non-root blob " root-id))))
    (:owner (struct/data-value blob))))


(defn- get-update-owners
  "Given a map of DatomUpdates, returns a set of the hash-ids of the
  keys which own the updated entities."
  [store updates]
  (->> (keys updates)
       (map (partial get-owner store))
       set))


(defn update-value
  "Constructs a new entity update value."
  [{:keys [time data]}]
  (schema/validate DatomUpdates data)
  (edn/typed-map
    update-type
    :time (or time (time/now))
    :data (into (sorted-map) data)))


(defn update->blob
  "Constructs a new update blob from the given args."
  [store sig-provider args]
  (apply
    sig/sign-value
    (update-value args)
    store
    sig-provider
    (get-update-owners store (:data args))))


(defn validate-update
  "Checks the structure and signatures on an entity update blob. Returns a blob
  record with verified signatures."
  [blob store]
  (schema/validate EntityUpdate (struct/data-value blob))
  (let [blob (sig/verify-sigs blob store)
        sigs (:data/signatures blob)
        owners (get-update-owners store (:data (struct/data-value blob)))
        missing (set/difference owners sigs)]
    (when-not (empty? missing)
      (throw (IllegalStateException.
               (str "Entity update blob " (:id blob)
                    " is missing signatures by some owning keys: "
                    (str/join " " missing)))))
    blob))



;; ## Transaction Datoms

(defn- map-datoms
  "Maps a constructor across a sequence of fragments, producing Datom records."
  [id time entity fragments]
  (map
    (fn [[op attr value]]
      (datom/->Datom op entity attr value id time))
    fragments))


(defn tx->datoms
  "Reads a sequence of datoms from a transaction blob."
  [{:keys [id] :as blob}]
  (let [{:keys [time data] :as tx} (struct/data-value blob)]
    (condp = (struct/data-type blob)
      root-type   (map-datoms id time id data)
      update-type (mapcat (partial apply map-datoms id time) data))))
