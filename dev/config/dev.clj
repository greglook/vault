(require '[vault.blob.store.file :refer [file-store]])

; Vault development system configuration.

(defaults
  :store    :store.file/local
  :catalog  nil
  :identity nil)


(component :store.file/local
  (file-store "dev/var/blobs"))
