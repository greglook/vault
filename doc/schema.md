# Vault Schemas

TODO: discuss EDN as the preferred serialization format for higher-level data.
TODO: determine how data 'types' are identified.

I've also had some random thoughts about somehow storing 'expected schemas' in
vault, which the system can use to validate data. This requires that there
somehow exists a notation for the 'latest' schema of various types, and tying
data to the schema it was stored under.

## Cryptographic Signatures

TODO: discuss how data structures are signed
- Public GPG key is stored as a blob and used as identity.
- Signatures follow the 'main' data in the blob as additional EDN values.

## The Blob Graph

There are two classes of reference to other blobs. Either the data structure is
_including_ the referenced blob as a part of the data, or it is _linking_ to an
entity represented by a permanode.

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
