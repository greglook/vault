(ns vault.util.pgp-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [vault.util.pgp :as pgp])
  (:import
    (org.bouncycastle.openpgp
      PGPSignature)))


(def test-pubring
  (io/file (io/resource "test-resources/pgp/pubring.gpg")))

(def test-secring
  (io/file (io/resource "test-resources/pgp/secring.gpg")))


(deftest utility-functions
  (testing "key-id coercion"
    (is (nil? (pgp/key-id nil))))
  (testing "public-key coercion"
    (is (thrown? IllegalArgumentException (pgp/public-key "a string"))))
  (testing "key-algorithm detection"
    (is (nil? (pgp/key-algorithm nil))))
    (is (= :rsa-general (pgp/key-algorithm :rsa-general))))


(deftest public-key-functions
  (let [pubrings (pgp/load-public-keyrings test-pubring)
        pubring (first pubrings)
        pubkey (first pubring)]
    (is (= 1 (count pubrings)))
    (is (= 2 (count pubring)))
    (testing "public key info"
      (let [info (pgp/key-info (pubring 0))]
        (are [k v] (= v (info k))
             :key-id "923b1c1c4392318a"
             :fingerprint "4C0F256D432975418FAB3D7B923B1C1C4392318A"
             :algorithm :rsa-general
             :strength 1024
             :master-key? true
             :encryption-key? true
             :user-ids ["Test User <test@vault.mvxcvi.com>"]))
      (let [info (pgp/key-info (pubring 1))]
        (are [k v] (= v (info k))
             :key-id "3f40edec41c6cb7d"
             :fingerprint "798A598943062D6C0D1D40F73F40EDEC41C6CB7D"
             :algorithm :rsa-general
             :strength 1024
             :master-key? false
             :encryption-key? true)))
    (is (identical? pubkey (pgp/public-key pubkey)))
    (testing "public key encoding"
      (let [encoded-key (pgp/encode-public-key pubkey)
            decoded-key (pgp/decode-public-key encoded-key)]
        (is (string? encoded-key))
        (is (instance? org.bouncycastle.openpgp.PGPPublicKey decoded-key))
        (is (= encoded-key (pgp/encode-public-key decoded-key)))))))


(deftest secret-key-functions
  (let [secrings (pgp/load-secret-keyrings test-secring)
        secring (first secrings)
        seckey (first secring)]
    (is (= 1 (count secrings)))
    (is (= 2 (count secring)))
    (testing "secret key info"
      (is (= :rsa-general (pgp/key-algorithm seckey)))
      (let [info (pgp/key-info (secring 0))]
        (are [k v] (= v (info k))
             :key-id "923b1c1c4392318a"
             :fingerprint "4C0F256D432975418FAB3D7B923B1C1C4392318A"
             :algorithm :rsa-general
             :strength 1024
             :master-key? true
             :private-key? true
             :encryption-key? true
             :signing-key? true
             :user-ids ["Test User <test@vault.mvxcvi.com>"]))
      (let [info (pgp/key-info (secring 1))]
        (are [k v] (= v (info k))
             :key-id "3f40edec41c6cb7d"
             :fingerprint "798A598943062D6C0D1D40F73F40EDEC41C6CB7D"
             :algorithm :rsa-general
             :strength 1024
             :master-key? false
             :private-key? true
             :encryption-key? true
             :signing-key? true)))))


(deftest signature-functions
  (let [data "cryptography is neat"
        id (pgp/key-id "3f40edec41c6cb7d")
        secrings (pgp/load-secret-keyrings test-secring)
        seckey (pgp/find-key id secrings)]
    (is (= id (pgp/key-id seckey)))
    (is (thrown? org.bouncycastle.openpgp.PGPException
                 (pgp/unlock-key seckey "wrong password")))
    (let [privkey (pgp/unlock-key seckey "test password")
          sig (pgp/sign data privkey)]
      (is (= (pgp/key-id privkey) (pgp/key-id sig)))
      (let [wrong-key (pgp/find-key "923b1c1c4392318a" secrings)]
        (is (thrown? IllegalArgumentException (pgp/verify data sig wrong-key))))
      (let [binary (pgp/encode-signature sig)
            sig' (pgp/decode-signature binary)]
        (is (bytes= (.getSignature sig)
                    (.getSignature sig')))
        (is (pgp/verify data sig' (pgp/public-key seckey)))))))
