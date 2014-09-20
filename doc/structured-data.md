Structured Data
===============

The blobs stored in Vault are interpreted in the _data layer_. Blob content is
classified into one of three types:
- Structured data is represented by _data blobs_.
- Cryptographic keys are stored as _key blobs_.
- All other content is considered _raw blobs_.

Data blobs are stored as [EDN](https://github.com/edn-format/edn) text. Key
blobs follow the OpenPGP standard and are stored as ASCII 'armored' text. Raw
blobs are not further interpreted.

## Data Blobs

Structured data in Vault is represented by blobs containing EDN values. These
blobs:
- MUST consist of UTF-8 encoded text
- MUST contain EDN-formatted data
- SHOULD NOT contain trailing whitespace
- MUST start with the line `#vault/data`
- MUST contain at least one 'primary' value
- MAY contain additional special values
    - only signatures, so far

Data blobs are recognized by the 'magic header' EDN tag prefix. The value
following the header is the _primary_ value of the blob. Additional _secondary_
EDN values may follow in order to provide things such as content signatures.

Values in data blobs are given a 'type'; for basic EDN values like strings,
numbers, vectors, etc., the type is just their class. For maps, the system
checks a special `:vault/type` key. If this key has a keyword value, that is
taken to be the type, otherwise it is treated as a generic map.

An example data blob:

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
{:key #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :signature #pgp/signature #bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMrV61su7iGPQsdXvZqBb2LKUhGku6ZQxqBYOvDdXaTmYIZJBY0CtAOlTe3NXn0kvnTuaPoA6fe6Ji1mndYUudKPpWWld9vzxIYpqnxL/ZtjgjWqkDf02q7M8ogSZ7dp09D1+P5mNnS4UOBTgpQuBNPWzoQ84QP/N0TaDMYYCyMuZaSsjZsSjZ0CcCm3GMIfTCkrkaBXOIMsHk4eddb3V7cswMGUjLY72k/NKhRQzmt5N/4jw/kI5gl1sN9+RSdp9caYkAumc1see44fJ1m+nOPfF8G79bpCQTKklnMhgdTOMJsCLZPdOuLxyxDJ2yte1lHKN/nlAOZiHFX4WXr0eYXV7NqjH4adA5LN0tkC5yMg86IRIY9B3QpkDPr5oQhlzfQZ+iAHX1MyfmhQCp8kmWiVsX8x/mZBLS0kHq6dJs//C1DoWEmvwyP7iIEPwEYFwMNQinOedu6ys0hQE0AN68WH9RgTfubKqRxeDi4+peNmg2jX/ws39C5YyaeJW7tO+1TslKhgoQFa61Ke9lMkcakHZeldZMaKu4Vg19OLAMFSiVBvmijZKuANJgmddpw0qr+hwAhVJBflB/txq8DylHvJJdyoezHTpRnPzkCSbNyalOxEtFZ8k6KX3i+JTYgpc2FLrn1Fa0zLGac7dIb88MMV8+Wt4H2d1c"
 :vault/type :vault/signature}
```

The signature must match the byte sequence of the UTF-8 encoding of the
characters which form the primary value in the blob, excluding the header.

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
