(ns vault.entity.datom-test
  (:require
    [clj-time.core :as time]
    [clojure.test :refer :all]
    [puget.printer :as puget]
    [vault.blob.core :as blob]
    [vault.data.core :as data]
    [vault.data.edn :as edn-data]
    [vault.data.test-keys :as keys]
    (vault.entity
      [core :as entity]
      [datom :as datom])))


(def blob-store keys/blob-store)


(deftest tx-blobs
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
    (if (< 0 (compare (:id root-a) (:id root-b)))
      (is (= [(entity/datom :attr/set (:id root-b) :title "Entity B" (:id update) t)
              (entity/datom :attr/add (:id root-b) :baz/xyz :abc (:id update) t)
              (entity/datom :attr/set (:id root-a) :title "Entity A" (:id update) t)
              (entity/datom :attr/set (:id root-a) :foo/bar 42 (:id update) t)]
             (datom/blob->datoms update)))
      (is (= [(entity/datom :attr/set (:id root-a) :title "Entity A" (:id update) t)
              (entity/datom :attr/set (:id root-a) :foo/bar 42 (:id update) t)
              (entity/datom :attr/set (:id root-b) :title "Entity B" (:id update) t)
              (entity/datom :attr/add (:id root-b) :baz/xyz :abc (:id update) t)]
             (datom/blob->datoms update))))))


(deftest entity-state
  (let [t (time/date-time 2014 5 14 3 20 36)
        root-id (blob/hash (.getBytes "foo"))]
    (is (thrown? RuntimeException
          (entity/entity-state root-id
            [(entity/datom :foo/bar root-id :attr "value" root-id t)]))
        "Unknown datom operation throws an exception.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id []))
        "No datoms should produce an empty map.")
    (is (= {:vault.entity/id root-id
            :title "Entity Title"}
           (entity/entity-state root-id
             [(entity/datom :attr/set root-id :title "Entity Title" root-id t)]))
        "Setting a value for an unset attribute should produce an attribute with that value.")
    (is (= {:vault.entity/id root-id
            :title "Title Revision"}
           (entity/entity-state root-id
             [(entity/datom :attr/set root-id :title "Entity Title" root-id t)
              (entity/datom :attr/set root-id :title "Title Revision" root-id t)]))
        "Setting an attribute to new value should produce an attribute with the new value.")
    (is (= {:vault.entity/id root-id
            :tags #{"foo"}}
           (entity/entity-state root-id
             [(entity/datom :attr/add root-id :tags "foo" root-id t)]))
        "Adding a value to unset attribute should produce a set with a single entry.")
    (is (= {:vault.entity/id root-id
            :tags #{"bar" "foo"}}
           (entity/entity-state root-id
             [(entity/datom :attr/add root-id :tags "foo" root-id t)
              (entity/datom :attr/add root-id :tags "bar" root-id t)]))
        "Adding two values to an attribute should produce a set with two entries.")
    (is (= {:vault.entity/id root-id
            :flexible #{"123" "abc"}}
           (entity/entity-state root-id
             [(entity/datom :attr/set root-id :flexible "123" root-id t)
              (entity/datom :attr/add root-id :flexible "abc" root-id t)]))
        "Adding a value to a single-valued attribute should produce a set with two entries.")
    (is (= {:vault.entity/id root-id
            :abc 123}
           (entity/entity-state root-id
             [(entity/datom :attr/set root-id :abc 123 root-id t)
              (entity/datom :attr/del root-id :foo nil root-id t)]))
        "Deleting an unset attribute should produce the same map.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/datom :attr/set root-id :foo "123" root-id t)
              (entity/datom :attr/del root-id :foo nil root-id t)]))
        "Deleting an attribute with nil value should remove any value for attribute.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/datom :attr/set root-id :foo "123" root-id t)
              (entity/datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting a value for an attribute with the same value should remove it.")
    (is (= {:vault.entity/id root-id
            :foo "123"}
           (entity/entity-state root-id
             [(entity/datom :attr/set root-id :foo "123" root-id t)
              (entity/datom :attr/del root-id :foo "abc" root-id t)]))
        "Deleting a value for an attribute with a different value should not remove it.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/datom :attr/add root-id :foo "123" root-id t)
              (entity/datom :attr/add root-id :foo "abc" root-id t)
              (entity/datom :attr/del root-id :foo nil root-id t)]))
        "Deleting a set attribute with nil value should remove the whole set.")
    (is (= {:vault.entity/id root-id}
           (entity/entity-state root-id
             [(entity/datom :attr/add root-id :foo "123" root-id t)
              (entity/datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting the only value from a set attribute should remove it.")
    (is (= {:vault.entity/id root-id
            :foo #{"abc"}}
           (entity/entity-state root-id
             [(entity/datom :attr/add root-id :foo "123" root-id t)
              (entity/datom :attr/add root-id :foo "abc" root-id t)
              (entity/datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting a value from a set attribute should remove the value.")))
