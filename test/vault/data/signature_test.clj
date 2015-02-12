(ns vault.data.signature-test
  (:require
    [clojure.test :refer :all]
    [vault.blob.content :as content]
    [vault.blob.store :as store]
    [vault.data.edn :as edn]
    [vault.data.signature :as sig]
    [vault.data.test-keys :as test-keys
     :refer [blob-store pubkey-id]]))


(deftest no-signature-blob
  (let [blob (-> {:foo 'bar}
                 (edn/data->blob
                   (constantly [{:baz 123}]))
                 (sig/verify-sigs blob-store))]
    (is (empty? (:data/signatures blob)))))


(deftest no-pubkey-blob
  (is (thrown? IllegalStateException
        (-> {:foo 'bar}
            (edn/data->blob
              (constantly
                [(edn/typed-map
                   :vault/signature
                   :key (content/hash (.getBytes "bazbar")))]))
            (sig/verify-sigs blob-store)))))


(deftest non-pubkey-blob
  (let [non-key (store/store! blob-store "foobar")]
    (is (thrown? IllegalStateException
          (-> {:foo 'bar}
              (edn/data->blob
                (constantly
                  [(edn/typed-map
                     :vault/signature
                     :key (:id non-key))]))
              (sig/verify-sigs blob-store))))))


(deftest signed-blob
  (let [value {:foo "bar", :baz :frobble, :alpha 12345}
        blob (-> value
                 (sig/sign-value blob-store test-keys/sig-provider pubkey-id)
                 (sig/verify-sigs blob-store))]
    (is (= :map (:data/type blob)))
    (is (= 2 (count (:data/values blob))))
    (is (= #{pubkey-id} (:data/signatures blob)))))
