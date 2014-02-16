(ns vault.data.signature
  "Signature handling functions."
  (:require
    [puget.data :refer [TaggedValue]]
    [vault.blob.core :as blob]
    [vault.data.format :as fmt]
    [mvxcvi.pgp.core :as pgp])
  (:import
    (org.bouncycastle.openpgp
      PGPPrivateKey
      PGPPublicKey
      PGPSecretKey
      PGPSignature)))


;; SIGNATURE RECORD

;#vault/signature
;^{:type :vault.data/signature}
;{:key #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
; :signature #pgp/signature #bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMrV61su7iGPQsdXvZqBb2LKUhGku6ZQxqBYOvDdXaTmYIZJBY0CtAOlTe3NXn0kvnTuaPoA6fe6Ji1mndYUudKPpWWld9vzxIYpqnxL/ZtjgjWqkDf02q7M8ogSZ7dp09D1+P5mNnS4UOBTgpQuBNPWzoQ84QP/N0TaDMYYCyMuZaSsjZsSjZ0CcCm3GMIfTCkrkaBXOIMsHk4eddb3V7cswMGUjLY72k/NKhRQzmt5N/4jw/kI5gl1sN9+RSdp9caYkAumc1see44fJ1m+nOPfF8G79bpCQTKklnMhgdTOMJsCLZPdOuLxyxDJ2yte1lHKN/nlAOZiHFX4WXr0eYXV7NqjH4adA5LN0tkC5yMg86IRIY9B3QpkDPr5oQhlzfQZ+iAHX1MyfmhQCp8kmWiVsX8x/mZBLS0kHq6dJs//C1DoWEmvwyP7iIEPwEYFwMNQinOedu6ys0hQE0AN68WH9RgTfubKqRxeDi4+peNmg2jX/ws39C5YyaeJW7tO+1TslKhgoQFa61Ke9lMkcakHZeldZMaKu4Vg19OLAMFSiVBvmijZKuANJgmddpw0qr+hwAhVJBflB/txq8DylHvJJdyoezHTpRnPzkCSbNyalOxEtFZ8k6KX3i+JTYgpc2FLrn1Fa0zLGac7dIb88MMV8+Wt4H2d1c"
; :target #vault/ref "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"}

(defrecord Signature
  [key ^PGPSignature signature target])


(extend-type Signature
  TaggedValue

  (tag [this] 'vault/signature)

  (value [this]
    (select-keys this #{:key :signature :target})))


(defn read-signature
  "Reads an encoded signature map."
  [value]
  (-> value
      (select-keys #{:key :signature :target})
      map->Signature))



;; XXXX

(defn- load-public-key
  "Loads a public key from a blob-store by its hash-id."
  [store hash-id]
  (let [encoded-pubkey (:content (blob/get store hash-id))]
    (when-not encoded-pubkey
      (throw (IllegalStateException.
               (str "No public key blob stored for " hash-id))))
    (let [pubkey (pgp/decode-public-key encoded-pubkey)]
      (when-not (instance? PGPPublicKey pubkey)
        (throw (IllegalStateException.
                 (str "Blob " hash-id " is not a PGP public key"))))
      pubkey)))


(defn- load-private-key
  "Obtains a private key for the given id."
  [provider key-id]
  ; FIXME: work out 'key-provider' function
  #_
  (let [privkey (pgp/get-private-key provider key-id)]
    (when-not (instance? PGPPrivateKey privkey)
      (throw (IllegalStateException.
               (str "Private key " (Long/toHexString key-id) " is not available"))))
    privkey))


(defn- sign-value-bytes
  "Signs byte data with the key looked up by the hash identifier."
  [blob-store key-provider value-bytes pubkey-hash]
  (let [pubkey (load-public-key blob-store pubkey-hash)
        privkey (load-private-key key-provider (pgp/key-id pubkey))
        pgp-sig (pgp/sign value-bytes privkey)]
    (map->Signature {:key pubkey-hash, :signature pgp-sig})))


(defn sign-value
  "Signs a clojure value with PGP keys. Returns the constructed string holding
  string holding the canonical value and signature."
  [blob-store key-provider value pubkey-hash & more]
  (let [pubkey-hashes (cons pubkey-hash more)
        value-bytes (fmt/value-bytes value)
        sign-fn (partial sign-value-bytes blob-store key-provider value-bytes)]
    (apply fmt/print-data-str value (map sign-fn pubkey-hashes))))


(defn verify-blob
  "Verifies that the inline signatures in a sequence of blob values are valid."
  [blob-store
   blob-values]
  ; TODO: implement
  nil)
