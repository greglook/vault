(ns vault.data.signature-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [mvxcvi.crypto.pgp :as pgp]
    (puget
      [data]
      [printer :as puget])
    [vault.blob.core :as blob]
    (vault.data
      [edn :as edn]
      [signature :as sig]
      [test-keys :as test-keys :refer [blob-store pubkey pubkey-id]]))
  (:import
    ; FIXME: why is this necessary??
    ; clojure.lang.Compiler$HostExpr.tagToClass(Compiler.java:1060)
    (org.bouncycastle.openpgp
      PGPPrivateKey
      PGPSecretKey)))


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
                   :key (blob/hash (.getBytes "bazbar")))]))
            (sig/verify-sigs blob-store)))))


(deftest non-pubkey-blob
  (let [non-key (blob/store! blob-store "foobar")]
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
