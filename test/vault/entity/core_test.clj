(ns vault.entity.core-test
  (:require
    [clj-time.core :as time]
    [clojure.test :refer :all]
    [puget.printer :as puget]
    [schema.core :as schema]
    [vault.blob.core :as blob]
    [vault.data.core :as data]
    [vault.data.edn :as edn-data]
    [vault.data.test-keys :as keys]
    [vault.entity.core :as entity]))


(def blob-store keys/blob-store)


(deftest root-records
  (let [owner (blob/hash (.getBytes "foo"))]
    (is (thrown? IllegalArgumentException
                 (entity/root-record {:owner nil})))
    (is (thrown? RuntimeException
                 (entity/root-record {:owner owner
                                      :data [{:malformed 'value}]})))
    (let [record (entity/root-record
                   {:owner owner})]
      (is (schema/validate entity/EntityRoot record))
      (is (entity/root? record)))
    (let [dt (time/date-time 2014 5 15 1 21 36)
          record (entity/root-record
                   {:owner owner
                    :id "foobar"
                    :time dt
                    :data [[:attr/set :title "Thing #1"]]})]
      (is (schema/validate entity/EntityRoot record))
      (is (= "foobar" (:id record)))
      (is (= dt (:time record)))
      (is (= [[:attr/set :title "Thing #1"]] (:data record)))
      (is (entity/root? record)))))


(deftest root-blobs
  (let [t (time/date-time 2014 5 15 1 21 36)
        root (entity/root-blob
               blob-store
               keys/sig-provider
               {:owner keys/pubkey-id
                :id "foobar"
                :time t
                :data [[:attr/set :title "Thing #1"]
                       [:attr/add :tags "foo"]
                       [:attr/add :tags "bar"]]})]
    ;(puget/with-color (edn-data/print-blob root))
    (is (entity/validate-root-blob root blob-store))
    (is (thrown? RuntimeException
          (entity/validate-root-blob
            (update-in root [:data/values] (partial take 1))
            blob-store)))
    (is (= [(entity/->Datom :attr/set (:id root) :title "Thing #1" (:id root) t)
            (entity/->Datom :attr/add (:id root) :tags "foo" (:id root) t)
            (entity/->Datom :attr/add (:id root) :tags "bar" (:id root) t)]
           (entity/blob->datoms root)))))


(deftest update-records
  (is (thrown? RuntimeException
               (entity/update-record {:data nil})))
  (is (thrown? RuntimeException
               (entity/update-record {:data [[:attr/set :title "Thing #2"]]})))
  (let [t (time/date-time 2014 5 14 3 20 36)
        record (entity/update-record
                 {:time t
                  :data {(blob/hash (.getBytes "barbaz"))
                         [[:attr/set :title "Thing #3"]]}})]
    (is (schema/validate entity/EntityUpdate record))
    (is (= t (:time record)))
    (is (entity/update? record))))


(deftest update-blobs
  (let [t (time/date-time 2014 5 14 3 20 36)
        root-a (->> {:owner keys/pubkey-id}
                    (entity/root-blob blob-store keys/sig-provider)
                    (blob/put! blob-store))
        root-b (->> {:owner keys/pubkey-id}
                    (entity/root-blob blob-store keys/sig-provider)
                    (blob/put! blob-store))
        updates {(:id root-a) [[:attr/set :title "Entity A"]
                               [:attr/set :foo/bar 42]]
                 (:id root-b) [[:attr/set :title "Entity B"]
                               [:attr/add :baz/xyz :abc]]}
        update (entity/update-blob
                 blob-store
                 keys/sig-provider
                 {:data updates
                  :time t})]
    #_
    (do
      (println "root-a" (str (:id root-a)))
      (puget/with-color (edn-data/print-blob root-a))
      (newline)
      (println "root-b" (str (:id root-b)))
      (puget/with-color (edn-data/print-blob root-b))
      (newline)
      (println "update" (str (:id update)))
      (puget/with-color (edn-data/print-blob update)))
    (is (entity/validate-update-blob update blob-store))
    (is (thrown? RuntimeException
          (entity/validate-update-blob
            (update-in update [:data/values] (partial take 1))
            blob-store)))
    (if (< 0 (compare (:id root-a) (:id root-b)))
      (is (= [(entity/->Datom :attr/set (:id root-b) :title "Entity B" (:id update) t)
              (entity/->Datom :attr/add (:id root-b) :baz/xyz :abc (:id update) t)
              (entity/->Datom :attr/set (:id root-a) :title "Entity A" (:id update) t)
              (entity/->Datom :attr/set (:id root-a) :foo/bar 42 (:id update) t)]
             (entity/blob->datoms update)))
      (is (= [(entity/->Datom :attr/set (:id root-a) :title "Entity A" (:id update) t)
              (entity/->Datom :attr/set (:id root-a) :foo/bar 42 (:id update) t)
              (entity/->Datom :attr/set (:id root-b) :title "Entity B" (:id update) t)
              (entity/->Datom :attr/add (:id root-b) :baz/xyz :abc (:id update) t)]
             (entity/blob->datoms update))))
    #_
    (let [datoms (mapcat entity/blob->datoms [root-a root-b update])]
      (puget/cprint (entity/entity-state (:id root-a) datoms))
      (puget/cprint (entity/entity-state (:id root-b) datoms)))))


(deftest entity-state
  (let [t (time/date-time 2014 5 14 3 20 36)
        root-id (blob/hash (.getBytes "foo"))]
    (is (thrown? RuntimeException
          (entity/entity-state root-id
            [(entity/->Datom :foo/bar root-id :attr "value" root-id t)]))
        "Unknown datom operation throws an exception.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id []))
        "No datoms should produce an empty map.")
    (is (= {:vault.entity/id root-id
            :title "Entity Title"}
           (entity/entity-state root-id
             [(entity/->Datom :attr/set root-id :title "Entity Title" root-id t)]))
        "Setting a value for an unset attribute should produce an attribute with that value.")
    (is (= {:vault.entity/id root-id
            :title "Title Revision"}
           (entity/entity-state root-id
             [(entity/->Datom :attr/set root-id :title "Entity Title" root-id t)
              (entity/->Datom :attr/set root-id :title "Title Revision" root-id t)]))
        "Setting an attribute to new value should produce an attribute with the new value.")
    (is (= {:vault.entity/id root-id
            :tags #{"foo"}}
           (entity/entity-state root-id
             [(entity/->Datom :attr/add root-id :tags "foo" root-id t)]))
        "Adding a value to unset attribute should produce a set with a single entry.")
    (is (= {:vault.entity/id root-id
            :tags #{"bar" "foo"}}
           (entity/entity-state root-id
             [(entity/->Datom :attr/add root-id :tags "foo" root-id t)
              (entity/->Datom :attr/add root-id :tags "bar" root-id t)]))
        "Adding two values to an attribute should produce a set with two entries.")
    (is (= {:vault.entity/id root-id
            :flexible #{"123" "abc"}}
           (entity/entity-state root-id
             [(entity/->Datom :attr/set root-id :flexible "123" root-id t)
              (entity/->Datom :attr/add root-id :flexible "abc" root-id t)]))
        "Adding a value to a single-valued attribute should produce a set with two entries.")
    (is (= {:vault.entity/id root-id
            :abc 123}
           (entity/entity-state root-id
             [(entity/->Datom :attr/set root-id :abc 123 root-id t)
              (entity/->Datom :attr/del root-id :foo nil root-id t)]))
        "Deleting an unset attribute should produce the same map.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/->Datom :attr/set root-id :foo "123" root-id t)
              (entity/->Datom :attr/del root-id :foo nil root-id t)]))
        "Deleting an attribute with nil value should remove any value for attribute.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/->Datom :attr/set root-id :foo "123" root-id t)
              (entity/->Datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting a value for an attribute with the same value should remove it.")
    (is (= {:vault.entity/id root-id
            :foo "123"}
           (entity/entity-state root-id
             [(entity/->Datom :attr/set root-id :foo "123" root-id t)
              (entity/->Datom :attr/del root-id :foo "abc" root-id t)]))
        "Deleting a value for an attribute with a different value should not remove it.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/->Datom :attr/add root-id :foo "123" root-id t)
              (entity/->Datom :attr/add root-id :foo "abc" root-id t)
              (entity/->Datom :attr/del root-id :foo nil root-id t)]))
        "Deleting a set attribute with nil value should remove the whole set.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/->Datom :attr/add root-id :foo "123" root-id t)
              (entity/->Datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting the only value from a set attribute should remove it.")
    (is (= {:vault.entity/id root-id
            :foo #{"abc"}}
           (entity/entity-state root-id
             [(entity/->Datom :attr/add root-id :foo "123" root-id t)
              (entity/->Datom :attr/add root-id :foo "abc" root-id t)
              (entity/->Datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting a value from a set attribute should remove the value.")))
