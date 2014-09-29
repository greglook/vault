(ns vault.entity.tx-test
  (:require
    [clj-time.core :as time]
    [clojure.test :refer :all]
    [puget.printer :as puget]
    [schema.core :as schema]
    (vault.blob
      [content :as content]
      [store :as store])
    [vault.data.test-keys :as keys]
    (vault.entity
      [datom :as datom]
      [schema :refer :all]
      [tx :as tx])))


(def blob-store keys/blob-store)


(deftest root-values
  (let [owner (content/hash (.getBytes "foo"))]
    (is (thrown? IllegalArgumentException
                 (tx/root-value {:owner nil})))
    (is (thrown? RuntimeException
                 (tx/root-value {:owner owner
                                      :data [{:malformed 'value}]})))
    (let [value (tx/root-value
                  {:owner owner})]
      (is (schema/validate EntityRoot value))
      (is (tx/root? value)))
    (let [dt (time/date-time 2014 5 15 1 21 36)
          value (tx/root-value
                  {:owner owner
                   :id "foobar"
                   :time dt
                   :data [[:attr/set :title "Thing #1"]]})]
      (is (schema/validate EntityRoot value))
      (is (= "foobar" (:id value)))
      (is (= dt (:time value)))
      (is (= [[:attr/set :title "Thing #1"]] (:data value)))
      (is (tx/root? value)))))


(deftest root-blobs
  (let [t (time/date-time 2014 5 15 1 21 36)
        root (tx/root->blob
               blob-store
               keys/sig-provider
               {:owner keys/pubkey-id
                :id "foobar"
                :time t
                :data [[:attr/set :title "Thing #1"]
                       [:attr/add :tags "foo"]
                       [:attr/add :tags "bar"]]})]
    (is (tx/validate-root root blob-store))
    (is (thrown? RuntimeException
          (tx/validate-root
            (update-in root [:data/values] (partial take 1))
            blob-store)))))


(deftest update-values
  (is (thrown? RuntimeException
               (tx/update-value {:data nil})))
  (is (thrown? RuntimeException
               (tx/update-value {:data [[:attr/set :title "Thing #2"]]})))
  (let [t (time/date-time 2014 5 14 3 20 36)
        value (tx/update-value
                {:time t
                 :data {(content/hash (.getBytes "barbaz"))
                        [[:attr/set :title "Thing #3"]]}})]
    (is (schema/validate EntityUpdate value))
    (is (= t (:time value)))
    (is (tx/update? value))))


(deftest update-blobs
  (let [t (time/date-time 2014 5 14 3 20 36)
        root-a (->> {:owner keys/pubkey-id}
                    (tx/root->blob blob-store keys/sig-provider)
                    (store/put! blob-store))
        root-b (->> {:owner keys/pubkey-id}
                    (tx/root->blob blob-store keys/sig-provider)
                    (store/put! blob-store))
        updates {(:id root-a) [[:attr/set :title "Entity A"]
                               [:attr/set :foo/bar 42]]
                 (:id root-b) [[:attr/set :title "Entity B"]
                               [:attr/add :baz/xyz :abc]]}
        update (tx/update->blob
                 blob-store
                 keys/sig-provider
                 {:data updates
                  :time t})]
    (is (tx/validate-update update blob-store))
    (is (thrown? RuntimeException
          (tx/validate-update
            (update-in update [:data/values] (partial take 1))
            blob-store)))))


(deftest tx-datoms
  (let [t (time/date-time 2014 5 14 3 20 36)
        root-a (->> {:owner keys/pubkey-id}
                    (tx/root->blob blob-store keys/sig-provider)
                    (store/put! blob-store))
        root-b (->> {:owner keys/pubkey-id}
                    (tx/root->blob blob-store keys/sig-provider)
                    (store/put! blob-store))
        updates {(:id root-a) [[:attr/set :title "Entity A"]
                               [:attr/set :foo/bar 42]]
                 (:id root-b) [[:attr/set :title "Entity B"]
                               [:attr/add :baz/xyz :abc]]}
        update (tx/update->blob
                 blob-store
                 keys/sig-provider
                 {:data updates
                  :time t})]
    (if (< 0 (compare (:id root-a) (:id root-b)))
      (is (= [(datom/->Datom :attr/set (:id root-b) :title "Entity B" (:id update) t)
              (datom/->Datom :attr/add (:id root-b) :baz/xyz :abc (:id update) t)
              (datom/->Datom :attr/set (:id root-a) :title "Entity A" (:id update) t)
              (datom/->Datom :attr/set (:id root-a) :foo/bar 42 (:id update) t)]
             (tx/tx->datoms update)))
      (is (= [(datom/->Datom :attr/set (:id root-a) :title "Entity A" (:id update) t)
              (datom/->Datom :attr/set (:id root-a) :foo/bar 42 (:id update) t)
              (datom/->Datom :attr/set (:id root-b) :title "Entity B" (:id update) t)
              (datom/->Datom :attr/add (:id root-b) :baz/xyz :abc (:id update) t)]
             (tx/tx->datoms update))))))
