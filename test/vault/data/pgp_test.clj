(ns vault.data.pgp-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.data.codec.base64 :as b64]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [vault.data.pgp :as pgp])
  (:import
    (org.bouncycastle.openpgp
      PGPSignature)))


(def test-pubring
  (io/file (io/resource "vault/data/pgp/pubring.gpg")))

(def test-secring
  (io/file (io/resource "vault/data/pgp/secring.gpg")))


(deftest reading-secring-file
  (let [secrings (pgp/load-secret-keyrings test-secring)
        secring (first secrings)]
    (is (= 1 (count secrings)))
    (is (= 2 (count secring)))
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
           :signing-key? true))))


(deftest signatures
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
      (let [binary (pgp/encode-signature sig)
            sig' (pgp/decode-signature binary)]
        (is (bytes= (.getSignature sig)
                    (.getSignature sig')))
        (is (pgp/verify data sig' (pgp/public-key seckey)))))))

