(ns vault.entity.core
  (:require
    [clj-time.core :as time]
    [clojure.set :as set]
    [clojure.string :as str]
    [schema.core :as schema]
    [vault.blob.core :as blob]
    [vault.data.core :as data])
  (:import
    org.joda.time.DateTime
    vault.blob.digest.HashID))


;; COMMON SCHEMAS

(def ^:const root-type   :vault.entity/root)
(def ^:const update-type :vault.entity/update)


(def DatomFragment
  "Schema for a fragment of a datom. Basically, a partial datom vector with
  :op, :attr, and :value."
  [(schema/one schema/Keyword "operation")
   (schema/one schema/Keyword "attribute")
   (schema/optional (schema/pred some? "some?") "value")])


(def DatomFragments
  "Schema for a vector of one or more datom fragments."
  [(schema/one DatomFragment "datoms")
   DatomFragment])


(def DatomUpdates
  "Schema for a map from entity ids to vectors of datom fragments."
  {HashID DatomFragments})



;; ENTITY ROOTS

(def EntityRoot
  "Schema for an entity root value."
  {data/type-key (schema/eq root-type)
   :id String
   :owner HashID
   :time DateTime
   (schema/optional-key :data) DatomFragments})


(defn root?
  "Determines whether the given value is an entity root."
  [value]
  (= root-type (data/type value)))


(defn- random-id!
  "Generates a random id string for entity roots."
  []
  (let [buf (byte-array 16)]
    (.nextBytes (java.security.SecureRandom.) buf)
    (.toString (BigInteger. 1 buf) 16)))


(defn root-record
  "Constructs a new entity root value."
  [{:keys [owner id time data]}]
  (when-not owner
    (throw (IllegalArgumentException. "Cannot create entity without owner")))
  (when data
    (schema/validate DatomFragments data))
  (cond->
    (data/typed-map
      root-type
      :id (or id (random-id!))
      :time (or time (time/now))
      :owner owner)
    data (assoc :data data)))


(defn root-blob
  "Constructs a new entity blob for the given owner."
  [blob-store sig-provider args]
  (data/sign-value
    (root-record args)
    blob-store
    sig-provider
    (:owner args)))


(defn validate-root-blob
  "Checks the structure and signatures on an entity root blob. Returns a blob
  record with verified signatures."
  [blob blob-store]
  (schema/validate EntityRoot (data/blob-value blob))
  (let [blob (data/verify-sigs blob blob-store)
        sigs (:data/signatures blob)
        owner (:owner (data/blob-value blob))]
    (when-not (contains? sigs owner)
      (throw (IllegalStateException.
               (str "Entity blob blob " (:id blob)
                    " does not have a valid signature by the owning key "
                    owner))))
    blob))



;; ENTITY UPDATES

(def EntityUpdate
  "Schema for an entity update value."
  {data/type-key (schema/eq update-type)
   :time DateTime
   :data DatomUpdates})


(defn update?
  "Determines whether the given value is an entity update."
  [value]
  (= update-type (data/type value)))


(defn get-owner
  "Looks up the owner for the given entity root id. Throws an exception if any
  of the ids is not an entity root."
  [blob-store root-id]
  (let [blob (data/read-blob (blob/get blob-store root-id))]
    (when-not blob
      (throw (IllegalArgumentException.
               (str "Cannot get owner for nonexistent entity " root-id))))
    (when-not (root? (data/blob-value blob))
      (throw (IllegalArgumentException.
               (str "Cannot get owner for non-root blob " root-id))))
    (:owner (data/blob-value blob))))


(defn- get-update-owners
  "Given a map of DatomUpdates, returns a set of the hash-ids of the
  keys which own the updated entities."
  [blob-store updates]
  (->> (keys updates)
       (map (partial get-owner blob-store))
       set))


(defn update-record
  "Constructs a new entity update value."
  [{:keys [time data]}]
  (schema/validate DatomUpdates data)
  (data/typed-map
    update-type
    :time (or time (time/now))
    :data (into (sorted-map) data)))


(defn update-blob
  "Constructs a new update blob from the given args."
  [blob-store sig-provider args]
  (apply
    data/sign-value
    (update-record args)
    blob-store
    sig-provider
    (get-update-owners blob-store (:data args))))


(defn validate-update-blob
  "Checks the structure and signatures on an entity update blob. Returns a blob
  record with verified signatures."
  [blob blob-store]
  (schema/validate EntityUpdate (data/blob-value blob))
  (let [blob (data/verify-sigs blob blob-store)
        sigs (:data/signatures blob)
        owners (get-update-owners blob-store (:data (data/blob-value blob)))
        missing (set/difference owners sigs)]
    (when-not (empty? missing)
      (throw (IllegalStateException.
               (str "Entity update blob " (:id blob)
                    " is missing signatures by some owning keys: "
                    (str/join " " missing)))))
    blob))



;; DATOM FUNCTIONS

(defrecord Datom [op entity attribute value tx time])


(defn blob->datoms
  "Converts a blob into a sequence of datoms."
  [blob]
  (let [map-datoms
        (fn [time entity fragments]
          (map
            (fn [[op attr value]]
              (Datom. op entity attr value (:id blob) time))
            fragments))
        record (data/blob-value blob)]
    (condp = (:data/type blob)
      root-type
      (map-datoms (:time record) (:id blob) (:data record))
      update-type
      (mapcat (partial apply map-datoms (:time record)) (:data record)))))


(defn apply-datom
  "Applies a datom to an entity state map to produce an updated state value.
  If the datom entity is not the same as the state value, it is ignored."
  [entity {:keys [op attribute value] :as datom}]
  (if (= (:vault.entity/id entity) (:entity datom))
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
          entity)))
    entity))


(defn entity-state
  "Given a sequence of datoms, return a map giving the 'current' state of some
  entity."
  [root-id datoms]
  (reduce apply-datom
    (sorted-map :vault.entity/id root-id)
    datoms))
