; Development blob store definitions.
{:default :local

 :local
 (file-store "dev/var/blobs")}


#_
{:default :main

 :main
 (->
   (s3-store
     "$USER-storage"
     :prefix "vault"
     :access-key (env :aws-access-key-id)
     :secret-key (env :aws-secret-access-key))
   (blob-encryptor
     :keyring "/home/$USER/.gnupg/secring.gpg"
     :encryption-key "069d6e9dde013b39"
     :compression :gzip)
   (blob-cache
     (file-store "/home/$USER/var/vault-cache")
     :limit (* 1024 1024 1024)) ; keep up to 1 GB on disk
   (blob-cache
     (memory-store)
     :limit (* 256 1024 1024))) ; keep up to 256 MB in memory
 }
