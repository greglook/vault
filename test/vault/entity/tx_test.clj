(ns vault.entity.tx-test
  (:require
    [clj-time.core :as time]
    [clojure.test :refer :all]
    [puget.printer :as puget]
    [schema.core :as schema]
    (vault.blob
      [content :as content]
      [store :as store])
    (vault.data
      [core :as data]
      [edn :as edn-data]
      [test-keys :as keys])
    (vault.entity
      [tx :as tx])))


(def blob-store keys/blob-store)


(deftest root-records
  (let [owner (content/hash (.getBytes "foo"))]
    (is (thrown? IllegalArgumentException
                 (tx/root-record {:owner nil})))
    (is (thrown? RuntimeException
                 (tx/root-record {:owner owner
                                      :data [{:malformed 'value}]})))
    (let [record (tx/root-record
                   {:owner owner})]
      (is (schema/validate tx/EntityRoot record))
      (is (tx/root? record)))
    (let [dt (time/date-time 2014 5 15 1 21 36)
          record (tx/root-record
                   {:owner owner
                    :id "foobar"
                    :time dt
                    :data [[:attr/set :title "Thing #1"]]})]
      (is (schema/validate tx/EntityRoot record))
      (is (= "foobar" (:id record)))
      (is (= dt (:time record)))
      (is (= [[:attr/set :title "Thing #1"]] (:data record)))
      (is (tx/root? record)))))


(deftest root-blobs
  (let [t (time/date-time 2014 5 15 1 21 36)
        root (tx/root-blob
               blob-store
               keys/sig-provider
               {:owner keys/pubkey-id
                :id "foobar"
                :time t
                :data [[:attr/set :title "Thing #1"]
                       [:attr/add :tags "foo"]
                       [:attr/add :tags "bar"]]})]
    (is (tx/validate-root-blob root blob-store))
    (is (thrown? RuntimeException
          (tx/validate-root-blob
            (update-in root [:data/values] (partial take 1))
            blob-store)))))


(deftest update-records
  (is (thrown? RuntimeException
               (tx/update-record {:data nil})))
  (is (thrown? RuntimeException
               (tx/update-record {:data [[:attr/set :title "Thing #2"]]})))
  (let [t (time/date-time 2014 5 14 3 20 36)
        record (tx/update-record
                 {:time t
                  :data {(content/hash (.getBytes "barbaz"))
                         [[:attr/set :title "Thing #3"]]}})]
    (is (schema/validate tx/EntityUpdate record))
    (is (= t (:time record)))
    (is (tx/update? record))))


(deftest update-blobs
  (let [t (time/date-time 2014 5 14 3 20 36)
        root-a (->> {:owner keys/pubkey-id}
                    (tx/root-blob blob-store keys/sig-provider)
                    (store/put! blob-store))
        root-b (->> {:owner keys/pubkey-id}
                    (tx/root-blob blob-store keys/sig-provider)
                    (store/put! blob-store))
        updates {(:id root-a) [[:attr/set :title "Entity A"]
                               [:attr/set :foo/bar 42]]
                 (:id root-b) [[:attr/set :title "Entity B"]
                               [:attr/add :baz/xyz :abc]]}
        update (tx/update-blob
                 blob-store
                 keys/sig-provider
                 {:data updates
                  :time t})]
    (is (tx/validate-update-blob update blob-store))
    (is (thrown? RuntimeException
          (tx/validate-update-blob
            (update-in update [:data/values] (partial take 1))
            blob-store)))))
