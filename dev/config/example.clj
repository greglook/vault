; Example Vault system configuration.

(defaults
  :store    :store.cache/memory
  :catalog  :index/catalog
  :identity :signature/keyring)



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



;; SQLite Indexes

(components
  (sqlite-graph-catalog
    :catalog/graph
    "/home/$USER/var/vault/graph.db"))

(components
  (sqlite-state-catalog
    :catalog/state
    "/home/$USER/var/vault/state.db"))

; Expand to the following, respectively:

(let [db-path "/home/$USER/var/vault/graph.db"]

  ; Tables

  (component :sqlite.table/blobs
    (sqlite-table
      db-path "blobs"
      :schema graph/node-schema
      :indexes {:typed [:type :label]}))

  (component :sqlite.table/links
    (sqlite-table
      db-path "links"
      :schema graph/link-schema
      :indexes {:forward [:source]
                :reverse [:target :type]}
      :unique-key [:source :target]))

  ; Indexes

  (component :sqlite.index/blob-stats
    (sqlite-index [:id])
    {:table :sqlite.table/blobs})

  (component :sqlite.index/blob-types
    (sqlite-index [:type :label])
    {:table :sqlite.table/blobs})

  (component :sqlite.index/links-forward
    (sqlite-index [:source])
    {:table :sqlite.table/links})

  (component :sqlite.index/links-reverse
    (sqlite-index [:target :type])
    {:table :sqlite.table/links})

  ; Catalog

  (component/using
    (index/catalog)
    {:blob-stats    :sqlite.index/blob-stats
     :blob-types    :sqlite.index/blob-types
     :links-forward :sqlite.index/links-forward
     :links-reverse :sqlite.index/links-reverse}))


(let [db-path "/home/$USER/var/vault/state.db"]

  ; Tables

  (component :sqlite.table/tx-log
    (sqlite-table
      db-path "tx_log"
      :schema tx/tx-schema
      :indexes {:history [:owner :time]}))

  (component :sqlite.table/datoms-all
    (sqlite-table
      db-path "datoms_all"
      :schema tx/datom-schema
      :indexes {:eavt [:entity :attribute :value :time]
                :aevt [:attribute :entity :value :time]}))

  ; only 'indexed' attrs
  (component :sqlite.table/datoms-avet
    (sqlite-table
      db-path "datoms_avet"
      :schema tx/datom-schema
      :indexes {:avet [:attribute :value :entity :time]}))

  ; only vault/ref values (reverse index)
  (component :sqlite.table/datoms-vaet
    (sqlite-table
      db-path "datoms_vaet"
      :schema tx/datom-schema
      :indexes {:vaet [:value :attribute :entity :time]}))

  ; Indexes

  (component :sqlite.index/tx-log
    (sqlite-index [:owner :time])
    {:table :sqlite.table/tx-log})

  (component :sqlite.index/eavt
    (sqlite-index [:entity :attribute :value :time])
    {:table :sqlite.table/datoms-all})

  (component :sqlite.index/aevt
    (sqlite-index [:attribute :entity :value :time])
    {:table :sqlite.table/datoms-all})

  (component :sqlite.index/avet
    (sqlite-index [:attribute :value :entity :time])
    {:table :sqlite.table/datoms-avet})

  (component :sqlite.index/vaet
    (sqlite-index [:value :attribute :entity :time])
    {:table :sqlite.table/datoms-vaet})

  ; Catalog

  (component/using
    (index/catalog)
    {:tx-log :sqlite.index/tx-log
     :eavt   :sqlite.index/eavt
     :aevt   :sqlite.index/aevt
     :avet   :sqlite.index/avet
     :vaet   :sqlite.index/vaet}))



;; Security Configuration

(component :signature/keyring
  (sig/keyring-sig-provider
    :sha1
    (pgp/load-secret-keyring "/home/$USER/.gnupg/secring.gpg")
    pinentry/ask-pass))
