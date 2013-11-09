# Structured Data

Data blobs:
- MUST be UTF-8 encoded text
- MUST contain EDN formatted data
- SHOULD NOT contain leading or trailing whitespace
- MUST contain at least one value ('primary' value)
- MAY contain additional special values
    - only signatures, so far

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
