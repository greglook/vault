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
