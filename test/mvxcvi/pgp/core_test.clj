(ns mvxcvi.pgp.core-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [mvxcvi.pgp.core :as pgp]
    [mvxcvi.pgp.keyring :as keyring]))


(def test-keyring
  (keyring/pgp-keyring
    (io/resource "test-resources/pgp/pubring.gpg")
    (io/resource "test-resources/pgp/secring.gpg")))

(def pubkey
  (pgp/get-public-key test-keyring "923b1c1c4392318a"))

(def seckey
  (pgp/get-secret-key test-keyring "3f40edec41c6cb7d"))


(deftest public-key-info
  (let [info (pgp/key-info pubkey)]
    (are [k v] (= v (info k))
      :key-id "923b1c1c4392318a"
      :fingerprint "4C0F256D432975418FAB3D7B923B1C1C4392318A"
      :algorithm :rsa-general
      :strength 1024
      :master-key? true
      :encryption-key? true
      :user-ids ["Test User <test@vault.mvxcvi.com>"])))


(deftest secret-key-info
  (let [info (pgp/key-info seckey)]
    (are [k v] (= v (info k))
      :key-id "3f40edec41c6cb7d"
      :fingerprint "798A598943062D6C0D1D40F73F40EDEC41C6CB7D"
      :algorithm :rsa-general
      :strength 1024
      :master-key? false
      :private-key? true
      :encryption-key? true
      :signing-key? true)))


(deftest utility-functions
  (testing "key-id coercion"
    (is (nil? (pgp/key-id nil))))
  (testing "public-key coercion"
    (is (identical? pubkey (pgp/public-key pubkey)))
    (is (thrown? IllegalArgumentException (pgp/public-key "a string"))))
  (testing "key-algorithm detection"
    (is (nil? (pgp/key-algorithm nil)))
    (is (= :rsa-general (pgp/key-algorithm seckey)))
    (is (= :rsa-general (pgp/key-algorithm :rsa-general)))))





#_
(deftest public-key-encoding
  (let [encoded-key (pgp/encode-public-key pubkey)
        decoded-key (pgp/decode-public-key encoded-key)]
    (is (string? encoded-key))
    (is (instance? org.bouncycastle.openpgp.PGPPublicKey decoded-key))
    (is (= encoded-key (pgp/encode-public-key decoded-key)))))


#_
(deftest signature-functions
  (let [data "cryptography is neat"
        id (pgp/key-id "3f40edec41c6cb7d")
        secrings (keyring/load-secret-keyrings test-secring)
        seckey (keyring/find-key id secrings)]
    (is (= id (pgp/key-id seckey)))
    (is (thrown? org.bouncycastle.openpgp.PGPException
                 (pgp/unlock-key seckey "wrong password")))
    (let [privkey (pgp/unlock-key seckey "test password")
          sig (pgp/sign data privkey)]
      (is (= (pgp/key-id privkey) (pgp/key-id sig)))
      (let [wrong-key (keyring/find-key "923b1c1c4392318a" secrings)]
        (is (thrown? IllegalArgumentException (pgp/verify data sig wrong-key))))
      (let [binary (pgp/encode-signature sig)
            sig' (pgp/decode-signature binary)]
        (is (bytes= (.getSignature sig)
                    (.getSignature sig')))
        (is (pgp/verify data sig' (pgp/public-key seckey)))))))
