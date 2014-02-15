(ns mvxcvi.pgp.keyring-test
  (:require
    #_ [byte-streams :refer [bytes=]]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    (mvxcvi.pgp
      [core :as pgp]
      [keyring :as keyring])))


(def test-keyring
  (keyring/pgp-keyring
    (io/resource "test-resources/pgp/pubring.gpg")
    (io/resource "test-resources/pgp/secring.gpg")))



