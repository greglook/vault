(ns vault.data-test
  (:require [clojure.test :refer :all]
            [vault.data :refer :all]))


(deftest total-ordering) ; TODO: implement


(deftest built-in-tagged-values
  (testing "TaggedValue"
    (are [data t v] (and (= t (tag data)) (= v (value data)))
         (byte-array 10)
         'bin "AAAAAAAAAAAAAA=="

         (java.util.Date. 1383271402749)
         'inst "2013-11-01T02:03:22.749-00:00"

         (java.util.UUID/fromString "96d91316-53b9-4800-81c1-97ae9f4b86b0")
         'uuid "96d91316-53b9-4800-81c1-97ae9f4b86b0"

         (java.net.URI. "http://en.wikipedia.org/wiki/Uniform_resource_identifier")
         'uri "http://en.wikipedia.org/wiki/Uniform_resource_identifier")))


(deftest bin-reading
  (let [byte-arr (.getBytes "foobarbaz")
        value-str (value byte-arr)
        read-arr (read-bin value-str)]
    (is (= (count byte-arr) (count read-arr)))
    (is (= (seq byte-arr) (seq read-arr)))))


(deftest uri-reading
  (let [uri (java.net.URI. "urn:isbn:0-486-27557-4")]
    (is (= uri (read-uri (value uri))))))
