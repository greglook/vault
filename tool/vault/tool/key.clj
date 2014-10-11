(ns vault.tool.key
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [mvxcvi.crypto.pgp :as pgp]
    [mvxcvi.directive :refer [fail print-err]]
    [vault.blob.store :as store]
    [vault.data.key :as key]))


(defn import-key
  [opts args]
  (let [store (:blob-store opts)
        [path key-id] args
        keyring (pgp/load-public-keyring (io/file path))]
    (when-not keyring
      (fail (str "No public keyring found at " path)))
    (let [matched-keys (filter #(.contains (:key-id (pgp/key-info %))
                                           (str/lower-case key-id))
                               (pgp/list-public-keys keyring))]
      (when-not (seq matched-keys)
        (fail (str "No public key found matching id " key-id)))
      (when (< 1 (count matched-keys))
        (fail (str (count matched-keys) " keys match id " key-id ":\n"
                   (str/join \newline (map (comp :key-id pgp/key-info) matched-keys)))))
      (let [blob (key/key->blob (first matched-keys))]
        (store/put! store blob)
        (println (:id blob))))))
