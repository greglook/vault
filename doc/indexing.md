# Blob Indexing

A common use case for a storage system is to _search_ it for data matching
certain attributes. While it is possible to exhaustively scan the entire data
store and calculate the relevant properties each time a query is performed, a
much better aproach is to _index_ the data.

In general, indexing represents a _view_ of the stored data which caches
desired search properties in a rapidly-accessible form. Indexes are **not**
authoritative stores of the blob data, and should not store blob contents.
Instead, the index might contain a listing of all permanodes which represent
"picture" files and their associated dimensions.

Since indexes are not intended to be durable, it is fine to delete and rebuild
them at any time. Indexes can be treated as a type of blobstore which does not
support `get`.

## Implementations

Indexes can be implemented on many kinds of databases. Early support will
probably consist of an in-memory implementation and later a SQLite3-backed
index.

## Uses

The following are use-cases for specific indices on the data. Still need to
understand indexing better.

* What permanodes are owned by a key?
* What permanodes have been affected by claims made by a key?
* What claims have been made about a permanode over time?
* What claims have been made about a specific attribute of a permanode over time?

Could handle this with a Claims table:
  string        claim-ref
  int           subclaim
  timestamp     time
  string        key-id
  string        permanode-ref
  string        type
  string        attr
  string        value

Primary key:
    [claim-ref subclaim]
Indices:
    [key-id time]
    [permanode-ref key-id time] (not convinced this is ideal)
    [permanode-ref attr time]
