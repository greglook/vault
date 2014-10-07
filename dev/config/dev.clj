; Vault development system configuration.

(defaults
  :store    :store.cache/memory
  :catalog  :index/catalog
  :identity :signature/keyring)



;; Local Blob Storage

(component :store.file/local
  (file-store "dev/var/blobs"))



;; Memory Indexes

(component :index.memory/blobs
  (memory-index query/blob-stats))

(component :index.memory/links
  (memory-index query/blob-links))

(component :index.memory/tx-log
  (memory-index query/tx-log))

(component :index.memory/datoms
  (memory-index query/entity-datoms))

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
