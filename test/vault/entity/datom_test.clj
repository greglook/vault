(ns vault.entity.datom-test
  (:require
    [clj-time.core :as time]
    [clojure.test :refer :all]
    [vault.blob.content :as content]
    [vault.entity.datom :as datom]))


(deftest entity-state
  (let [t (time/date-time 2014 5 14 3 20 36)
        root-id (content/hash (.getBytes "foo"))]
    (is (thrown? RuntimeException
          (datom/entity-state root-id
            [(datom/->Datom :foo/bar root-id :attr "value" root-id t)]))
        "Unknown datom operation throws an exception.")
    (is (= {:vault.entity/id root-id}
           (datom/entity-state root-id []))
        "No datoms should produce an empty map.")
    (is (= {:vault.entity/id root-id
            :title "Entity Title"}
           (datom/entity-state root-id
             [(datom/->Datom :attr/set root-id :title "Entity Title" root-id t)]))
        "Setting a value for an unset attribute should produce an attribute with that value.")
    (is (= {:vault.entity/id root-id
            :title "Title Revision"}
           (datom/entity-state root-id
             [(datom/->Datom :attr/set root-id :title "Entity Title" root-id t)
              (datom/->Datom :attr/set root-id :title "Title Revision" root-id t)]))
        "Setting an attribute to new value should produce an attribute with the new value.")
    (is (= {:vault.entity/id root-id
            :tags #{"foo"}}
           (datom/entity-state root-id
             [(datom/->Datom :attr/add root-id :tags "foo" root-id t)]))
        "Adding a value to unset attribute should produce a set with a single entry.")
    (is (= {:vault.entity/id root-id
            :tags #{"bar" "foo"}}
           (datom/entity-state root-id
             [(datom/->Datom :attr/add root-id :tags "foo" root-id t)
              (datom/->Datom :attr/add root-id :tags "bar" root-id t)]))
        "Adding two values to an attribute should produce a set with two entries.")
    (is (= {:vault.entity/id root-id
            :flexible #{"123" "abc"}}
           (datom/entity-state root-id
             [(datom/->Datom :attr/set root-id :flexible "123" root-id t)
              (datom/->Datom :attr/add root-id :flexible "abc" root-id t)]))
        "Adding a value to a single-valued attribute should produce a set with two entries.")
    (is (= {:vault.entity/id root-id
            :abc 123}
           (datom/entity-state root-id
             [(datom/->Datom :attr/set root-id :abc 123 root-id t)
              (datom/->Datom :attr/del root-id :foo nil root-id t)]))
        "Deleting an unset attribute should produce the same map.")
    (is (= {:vault.entity/id root-id}
           (datom/entity-state root-id
             [(datom/->Datom :attr/set root-id :foo "123" root-id t)
              (datom/->Datom :attr/del root-id :foo nil root-id t)]))
        "Deleting an attribute with nil value should remove any value for attribute.")
    (is (= {:vault.entity/id root-id}
           (datom/entity-state root-id
             [(datom/->Datom :attr/set root-id :foo "123" root-id t)
              (datom/->Datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting a value for an attribute with the same value should remove it.")
    (is (= {:vault.entity/id root-id
            :foo "123"}
           (datom/entity-state root-id
             [(datom/->Datom :attr/set root-id :foo "123" root-id t)
              (datom/->Datom :attr/del root-id :foo "abc" root-id t)]))
        "Deleting a value for an attribute with a different value should not remove it.")
    (is (= {:vault.entity/id root-id}
           (datom/entity-state root-id
             [(datom/->Datom :attr/add root-id :foo "123" root-id t)
              (datom/->Datom :attr/add root-id :foo "abc" root-id t)
              (datom/->Datom :attr/del root-id :foo nil root-id t)]))
        "Deleting a set attribute with nil value should remove the whole set.")
    (is (= {:vault.entity/id root-id}
           (datom/entity-state root-id
             [(datom/->Datom :attr/add root-id :foo "123" root-id t)
              (datom/->Datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting the only value from a set attribute should remove it.")
    (is (= {:vault.entity/id root-id
            :foo #{"abc"}}
           (datom/entity-state root-id
             [(datom/->Datom :attr/add root-id :foo "123" root-id t)
              (datom/->Datom :attr/add root-id :foo "abc" root-id t)
              (datom/->Datom :attr/del root-id :foo "123" root-id t)]))
        "Deleting a value from a set attribute should remove the value.")))
