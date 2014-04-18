Blob Indexing
=============

A common use case for a storage system is to _search_ it for data matching
certain attributes. While it is possible to exhaustively scan the entire data
store and calculate the relevant properties each time a query is performed, a
much better aproach is to _index_ the data.

In general, indexing represents a _view_ of the stored data which caches
desired search criteria in a rapidly-accessible form. Indexes are **not**
authoritative stores of the blob data, and should not store blob contents.
Instead, the index might contain a listing of all objects which represent
"picture" files and their associated dimensions.

Since indexes are not intended to be durable, it is fine to delete and rebuild
them at any time. Indexes can be treated as a type of blobstore which does not
support the `open` operation.

## Interface

Indexers can be thought of as _views_ of the blob data. It should be possible to
declaratively specify the transformations to produce each view, a predicate
which determines whether to apply the given transformation to a blob, and a
number of _indexes_ on that view to optimize.

```clojure
{:attrs [blob key data ^Date time]
 :indexes [[data key]
           [key time]]
 :predicate #(isa? (type %) :vault/signature)
 :view (fn [blobref sig]
         {:blob blobref
          :key (:key sig)
          :data (or (:data sig) blobref)})}
```

The above definition indexes signature data, enabling lookups for:
- what keys have signed data in a given blob?
- what blobs have been signed by a given key over time?

## Implementations

Indexes can be implemented on many kinds of databases. Early support will
probably consist of an in-memory implementation and later a SQLite3-backed
index.

## Misc

The following are use-cases for specific indices on the data. Still need to
understand indexing better.

* What objects are owned by a key?
* What objects have been affected by changes made by a key?
* What changes have been made about an object over time?

Could handle this with a `changes` table:
<pre>
  string        change-ref
  int           index
  timestamp     time
  string        key-ref
  string        object-ref
  string        type
  string        attr
  string        value
</pre>

Primary key:
- change-ref, index
Indices:
- key-ref, time
- object-ref, key-ref, time (not convinced this is ideal)

It would be good if users could specify attributes to be indexed for certain
blob types somehow.
