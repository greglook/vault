(ns vault.data.pgp-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.data.codec.base64 :as b64]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [vault.data.pgp :as pgp]))


(def test-pubring
  (io/file (io/resource "vault/data/test_keys/pubring.gpg")))

(def test-secring
  (io/file (io/resource "vault/data/test_keys/secring.gpg")))



(deftest secring-loading
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




#_
(def test-data
  {:content "foobarbaz"
   :signature (b64/decode "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMrV61su7iGPQsdXvZqBb2LKUhGku6ZQxqBYOvDdXaTmYIZJBY0CtAOlTe3NXn0kvnTuaPoA6fe6Ji1mndYUudKPpWWld9vzxIYpqnxL/ZtjgjWqkDf02q7M8ogSZ7dp09D1+P5mNnS4UOBTgpQuBNPWzoQ84QP/N0TaDMYYCyMuZaSsjZsSjZ0CcCm3GMIfTCkrkaBXOIMsHk4eddb3V7cswMGUjLY72k/NKhRQzmt5N/4jw/kI5gl1sN9+RSdp9caYkAumc1see44fJ1m+nOPfF8G79bpCQTKklnMhgdTOMJsCLZPdOuLxyxDJ2yte1lHKN/nlAOZiHFX4WXr0eYXV7NqjH4adA5LN0tkC5yMg86IRIY9B3QpkDPr5oQhlzfQZ+iAHX1MyfmhQCp8kmWiVsX8x/mZBLS0kHq6dJs//C1DoWEmvwyP7iIEPwEYFwMNQinOedu6ys0hQE0AN68WH9RgTfubKqRxeDi4+peNmg2jX/ws39C5YyaeJW7tO+1TslKhgoQFa61Ke9lMkcakHZeldZMaKu4Vg19OLAMFSiVBvmijZKuANJgmddpw0qr+hwAhVJBflB/txq8DylHvJJdyoezHTpRnPzkCSbNyalOxEtFZ8k6KX3i+JTYgpc2FLrn1Fa0zLGac7dIb88MMV8+Wt4H2d1c")
   :key "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"})


