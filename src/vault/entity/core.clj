(ns vault.entity.core
  (:require
    [clj-time.core :as time]
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
   (schema/one (schema/pred some? "some?") "value")])


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
  ; TODO: explicitly check owner is a public key?
  ; Happens automatically during signing anyway...
  (data/sign-value
    (root-record args)
    blob-store
    sig-provider
    (:owner args)))


(defn validate-root-blob
  "Checks the structure and signatures on an entity root blob. Returns a blob
  record with verified signatures."
  [root blob-store]
  (schema/validate EntityRoot (data/blob-value root))
  (let [root (data/verify-sigs root blob-store)
        sigs (:data/signatures root)
        owner (:owner (data/blob-value root))]
    (when-not (contains? sigs owner)
      (throw (IllegalStateException.
               (str "Entity root blob " (:id root)
                    " does not have a valid signature by the owning key "
                    owner))))
    root))



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


(defn update-record
  "Constructs a new entity update value."
  [{:keys [time data]}]
  (schema/validate DatomUpdates data)
  (data/typed-map
    update-type
    :time (or time (time/now))
    :data data))


(defn update-blob
  "Constructs a new update blob from the given args."
  [blob-store sig-provider args]
  (let [owners (sort (map (partial get-owner blob-store)
                          (keys (:data args))))]
    (apply
      data/sign-value
      (update-record args)
      blob-store
      sig-provider
      owners)))



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
        data (:data (data/blob-value blob))]
    (case (:data/type blob)
      :vault.entity/root
      (map-datoms (:time data) (:id blob) data)
      :vault.entity/update
      (mapcat (partial apply map-datoms (:time data)) data))))
