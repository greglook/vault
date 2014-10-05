; Vault development storage configuration.

;; S3 Encrypted Blob Storage

(defn s3-storage
  [dir]
  (s3-store
    "vault-storage"
    :prefix dir
    :access-key (env :aws-access-key-id)
    :secret-key (env :aws-secret-access-key)))


(component :store.s3/crypt-data
  (s3-storage "data"))

(component :store.s3/crypt-meta
  (s3-storage "meta"))

(component :store.s3/crypt
  (blob-encryptor
    :keyring "/home/$USER/.gnupg/secring.gpg"
    :encryption-key "069d6e9dde013b39"
    :compression :gzip)
  {:data-store :store.s3/crypt-main
   :meta-store :store.s3/crypt-meta})



;; Local Filesystem Blob Cache

(component :store.file/local
  (file-store "/home/$USER/var/vault/blobs"))

(component :store.cache/file
  ; keep up to 10 GB on disk
  (blob-cache :limit (* 10 1024 1024 1024))
  {:main-store  :store.s3/crypt
   :cache-store :store.file/local})



;; Memory Blob Cache

(component :store.memory/cache
  (memory-store))

(component :store.cache/memory
  ; keep up to 256 MB in memory
  (blob-cache :limit (* 256 1024 1024))
  {:main-store  :store.cache/file
   :cache-store :store.memory/cache})



;; Index Configuration

(defn sqlite-table
  [table recipe]
  (sqlite-index
    recipe
    "/home/$USER/var/vault/catalog.db"
    :table table))


(component :index.sqlite/blobs
  (sqlite-table "blobs" query/blob-stats))

(component :index.sqlite/links
  (sqlite-table "links" query/blob-links))

(component :index.sqlite/tx-log
  (sqlite-table "tx_log" query/tx-log))

(component :index.sqlite/datoms
  (sqlite-table "datoms" query/entity-datoms))

(component :index/catalog
  (index/catalog)
  {:blobs  :index.sqlite/blobs
   :links  :index.sqlite/links
   :txns   :index.sqlite/tx-log
   :datoms :index.sqlite/datoms})



;; Security Configuration

(component :signature/keyring
  (sig/keyring-sig-provider
    :sha1
    (pgp/load-secret-keyring "/home/$USER/.gnupg/secring.gpg")
    pinentry/ask-pass))



;; Component Selections

(defaults
  :store    :store.cache/memory
  :catalog  :index/catalog
  :identity :signature/keyring)
