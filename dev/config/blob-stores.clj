; Development blob store definitions.
{:default :local

 :local
 (file-store "dev/var/blobs")}

#_
(component/system-map

  ;; Blob Storage Configuration

  :store.s3/crypt-data
  (store/s3
    "vault-storage"
    :prefix "data"
    :access-key (env :aws-access-key-id)
    :secret-key (env :aws-secret-access-key))

  :store.s3/crypt-meta
  (store/s3
    "vault-storage"
    :prefix "meta")

  :store.s3/crypt
  (component/using
    (store/encryptor
      :keyring "/home/$USER/.gnupg/secring.gpg"
      :encryption-key "069d6e9dde013b39"
      :compression :gzip)
    {:data-store :store.s3/crypt-main
     :meta-store :store.s3/crypt-meta})

  :store.file/local
  (store/file "/home/$USER/var/vault/blobs")

  :store.cache/file
  (component/using
    ; keep up to 10 GB on disk
    (store/cache :limit (* 10 1024 1024 1024))
    {:main-store :store.s3/crypt
     :cache-store :store.file/local})

  :store.memory/cache
  (memory-store)

  :store.cache/memory
  (component/using
    ; keep up to 256 MB in memory
    (store/cache :limit (* 256 1024 1024))
    {:main-store :store.cache/file
     :cache-store :store.memory/cache})


  ;; Index Configuration

  :index.sqlite/blobs
  (index/sqlite
    query/blob-stats
    "/home/$USER/var/vault/catalog.db"
    :table "blobs")

  :index.sqlite/links
  (index/sqlite
    query/blob-links
    "/home/$USER/var/vault/catalog.db"
    :table "links")

  :index.sqlite/tx-log
  (index/sqlite
    query/tx-log
    "/home/$USER/var/vault/catalog.db"
    :table "tx")

  :index.sqlite/datoms
  (index/sqlite
    query/entity-datoms
    "/home/$USER/var/vault/catalog.db"
    :table "datoms")

  :index/catalog
  (component/using
    (index/catalog)
    {:blobs  :index.sqlite/blobs
     :links  :index.sqlite/links
     :txns   :index.sqlite/tx-log
     :datoms :index.sqlite/datoms})


  ;; Security Configuration

  :signature/keyring
  (sig/keyring-sig-provider
    :sha1
    (pgp/load-secret-keyring "/home/$USER/.gnupg/secring.gpg")
    pinentry/ask-pass)


  ;; Component Selections

  :defaults
  {:store    :store.cache/memory
   :catalog  :index/catalog
   :identity :signature/keyring})
