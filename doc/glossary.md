Glossary
========

| Term       | Symbols | Types                        | Definition |
|------------|:-------:|------------------------------|------------|
| hash-id    | `id`    | `vault.blob.digest.HashID`   | A hash identifier for a blob of data.
| blob       | `blob`  | `vault.blob.store.Blob`      | A blob record with at least `:id` and `:content` fields.
| blob store | `store` | `vault.blob.store.BlobStore` | A system which stores blob data.
| data blob  | `data`  | `vault.blob.store.Blob`      | A blob which has been parsed by the data layer.
| public key | `public-key` | `org.bouncycastle.openpgp.PGPPublicKey` | The public half of a cryptographic keypair.
| signature  | `signature`  | `org.bouncycastle.openpgp.PGPSignature` | A cryptographic signature.
| index      | `idx` `index` | ??? | ???
| catalog    | `catalog`     | ??? | Collection of indexes at the system level.
