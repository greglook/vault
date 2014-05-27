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


(deftest update-records
  (is (thrown? RuntimeException
               (entity/update-record {:data nil})))
  (is (thrown? RuntimeException
               (entity/update-record {:data [[:attr/set :title "Thing #2"]]})))
  (let [dt (time/date-time 2014 5 14 3 20 36)
        record (entity/update-record
                 {:time dt
                  :data {(blob/hash (.getBytes "barbaz"))
                         [[:attr/set :title "Thing #3"]]}})]
    (is (schema/validate entity/EntityUpdate record))
    (is (= dt (:time record)))
    (is (entity/update? record))))


(deftest root-blobs
   (let [root (entity/root-blob
                keys/blob-store
                keys/sig-provider
                {:owner keys/pubkey-id
                 :id "foobar"
                 :time (time/date-time 2014 5 15 1 21 36)
                 :data [[:attr/set :title "Thing #1"]]})]
     (puget/with-color (edn-data/print-blob root))
     (is (entity/validate-root-blob root keys/blob-store))
     (is (thrown? RuntimeException
           (entity/validate-root-blob
             (update-in root [:data/values] (comp vector first))
             keys/blob-store)))))
