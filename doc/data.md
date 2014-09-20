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

This value has the type `:filesystem/file`. The `:content/bytes` key points to a
blob which directly contains the byte content of the file.

### Supported Tags

Vault supports several EDN tags in addition to those supported by default in
Clojure.

| Tag              | Type           | Definition |
|------------------|----------------|------------|
| `#bin`           | `byte[]`       | Binary data encoded in Base64.
| `#uri`           | `URI`          | Uniform Resource Identifier string.
| `#inst`          | `DateTime`     | An instant in time, rendered as an ISO-8601 string.
| `#vault/ref`     | `HashID`       | A hash identifier string for a blob of data.
| `#pgp/signature` | `PGPSignature` | A binary-encoded PGP signature packet.

The system _should_ provide a standard mechanism for registering additional tag
readers in API clients.

## Key Blobs

Vault makes use of the OpenPGP standard for public keys and digital signatures.
Users of Vault are identified by _public key_ - the key itself is stored
directly in the system as an ASCII 'armored' blob. The address of the key blob
serves as an unambiguous identifier of the person controlling the corresponding
private key.

```
-----BEGIN PGP PUBLIC KEY BLOCK-----
Version: BCPG v1.50

mI0EUr3KFwEEANAfzcKxWqBYhkUGo4xi6d2zZy2RAewFRKVp/BA2bEHLAquDnpn7
abgrpsCFbBW/LEwiMX6cfYLMxvGzbg5oTfQHMs27OsnKCqFas9UkT6DYS1PM9u4C
3qlJytK9AFQnSYOrSs8pe6VRdeHZb7FM+PawqH0cuoYfcMZiGAylddXhABEBAAG0
IVRlc3QgVXNlciA8dGVzdEB2YXVsdC5tdnhjdmkuY29tPoi4BBMBAgAiBQJSvcoX
AhsDBgsJCAcDAgYVCAIJCgsEFgIDAQIeAQIXgAAKCRCSOxwcQ5IxioerBACBfXLk
j4ryCBcmJ+jtL1/W5jUQ/E4LWW7gG34tkHBJk00YulXUe4lwY1x9//6zowVV7DvW
ndmjcb0R6Duw76+xLsnEKomW0aV+ydoTtFZ7bX0kChX/IegPLMB0OnenzOoWKieN
GLFUpzICH+i7Or5X2bmekHtnbHPfJmQAuvmUnrACAAA=
=3aKz
-----END PGP PUBLIC KEY BLOCK-----
```

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
