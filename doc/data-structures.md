Structured Data
===============

Higher-level data in Vault is represented by blobs containing EDN values. These
are known as _data blobs_ and form the _blob graph_. Data blobs:
- MUST consist of UTF-8 encoded text
- MUST contain EDN-formatted data
- SHOULD NOT contain trailing whitespace
- MUST start with the line `#vault/data`
- MUST contain at least one 'primary' value
- MAY contain additional special values
    - only signatures, so far

Data blobs are recognized by the 'magic header' EDN tag prefix. The value
following the header is the _primary_ value of the blob. Additional EDN values
may follow in order to provide things such as content signatures.

Values in data blobs are given a 'type'; for basic EDN values like strings,
numbers, vectors, etc., the type is just their class. For maps, the system
checks a special `:vault/type` key. If this key has a keyword value, that is
taken to be the type, otherwise it is treated as a generic map.

An example data blob:

```clojure
#vault/data
{:vault/type :vault.data/bytes
 :parts [{:size 1000}]}
```

## EDN Tags

Vault provides a few EDN tags for special types:

- `#vault/ref` : marks a hash identifier as a blobref
- `#vault/data` : identifies the primary value in a data blob

## The Blob Graph

There are two classes of reference to other blobs. Either the data structure is
_including_ the referenced blob as a part of the data, or it is _linking_ to an
entity represented by an object.

A data structure should be split into multiple blobs if a part of the data is
very large and will change less frequently than other parts of the data, or
equivalently, is likely to be shared frequently. As an example, the metadata
representing a filesystem directory includes metadata like the directory name,
permissions, access times, and so on. It also contains a set of children, which
may be quite large. If the set of children is included directly in the
directory metadata, then operations such as renaming cause the entire set to be
stored again, even though it didn't change. A better practice would be to
make the children attribute of the structure a `blobref` to a separately stored
static set of the children.
