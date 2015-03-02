Structured Data
===============

The blobs stored in Vault are interpreted in the _data layer_. Blob content is
classified into one of three types:
- Structured data is represented by _data blobs_.
- Cryptographic keys are stored as [_key blobs_](keys.md).
- All other content is considered _raw blobs_.

Data blobs are stored as EDN text. Key blobs follow the OpenPGP standard and are
stored as ASCII 'armored' text. Raw blobs are not further interpreted.

## Data Blobs

Structured data in Vault is represented by blobs containing UTF-8 encoded
[EDN](https://github.com/edn-format/edn) text. Data blobs are recognized by the
magic header `#vault/data` EDN tag. The value following the header is the
_primary_ value of the blob. Additional _secondary_ values may follow in order
to provide things such as content signatures.

Values in data blobs are given a 'type'; for basic EDN values like strings,
numbers, vectors, etc., the type is just their class. For maps, the system
checks a special `:vault/type` key. If this key has a keyword value, that is
taken to be the type, otherwise the data is treated as a generic map.

An example blob:

```clojure
#vault/data
{:name "foo.clj"
 :content #bytes/seq #vault/blob "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :posix/permissions 493 ; 0755
 :owner/id 1000
 :owner/name "user"
 :group/id 500
 :group/name "users"
 :time/change #inst "2013-10-23T20:06:13.000-00:00"
 :time/modify #inst "2013-10-25T09:13:24.000-00:00"}
```

The `:content` key points to a blob which directly contains the byte content of
the file.

### Supported Tags

Vault supports several EDN tags in addition to those supported by default in
Clojure.

| Tag              | Type           | Definition |
|------------------|----------------|------------|
| `#inst`          | `DateTime`     | An instant in time, rendered as an ISO-8601 string.
| `#bytes/bin`     | `byte[]`       | Binary data encoded in Base64.
| `#vault/blob`    | `HashID`       | A hash identifier string for a blob of data.
| `#pgp/signature` | `PGPSignature` | A binary-encoded PGP signature packet.

The system _should_ provide a standard mechanism for registering additional tag
readers in API clients.

## Blob Graph

A data structure should be split into multiple blobs if a part of the data is
very large, will change less frequently than other parts of the data, or
is likely to be shared frequently.

As an example, the metadata representing a filesystem directory includes
information like the directory name, permissions, access times, and so on. It
also contains a set of children, which may be quite large. If the set of
children is included directly in the directory metadata, then operations such as
renaming the directory cause the entire set to be stored again.

A better approach is be to make the children attribute of the structure a
hash-id reference to a separate static set of hash-ids, each of which point to a
further file or directory metadata blob.
