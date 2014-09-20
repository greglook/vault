Structured Data
===============

The blobs stored in Vault are interpreted in the _data layer_. Blob content is
classified into one of three types:
- Structured data is represented by _data blobs_.
- Cryptographic keys are stored as _key blobs_.
- All other content is considered _raw blobs_.

Data blobs are stored as EDN text. Key
blobs follow the OpenPGP standard and are stored as ASCII 'armored' text. Raw
blobs are not further interpreted.

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
{:vault/type :filesystem/file
 :name "foo.clj"
 :posix/permissions 493 ; 0755
 :owner/id 1000
 :owner/name "user"
 :group/id 500
 :group/name "users"
 :time/change #inst "2013-10-23T20:06:13.000-00:00"
 :time/modify #inst "2013-10-25T09:13:24.000-00:00"
 :content/bytes #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"}
```

This value has the type `:filesystem/file`. The `#vault/ref` tag marks a string
as a hash identifier; in this case, the `:content/bytes` key points to a blob
which directly contains the byte content of the file.

## Key Blobs

Vault makes use of the OpenPGP standard for public keys and digital signatures.
Users of Vault are identified by _public key_ - the key itself is stored
directly in the system as an ASCII 'armored' blob. The address of the key blob
serves as an unambiguous identifier of the person controlling the corresponding
private key.

TODO: add example

## Data Signatures

Users can sign data in the system with signature structures. Signatures are maps
which follow the primary value in a data blob, and reference the public key blob
of the owner.

```clojure
{:vault/type :vault/signature
 :key #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :signature #pgp/signature #bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMr..."}
```

The signature must match the UTF-8 encoded byte sequence of the characters which
form the primary value in the blob (not including the header).

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
