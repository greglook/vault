Data Indexing
=============

A common use case for a storage system is to search it for data matching some
pattern. While it is possible to exhaustively scan the entire data store and
calculate the relevant properties each time a query is performed, a much better
aproach is to _index_ the data.

In general, indexing represents a _view_ of the stored data which caches
desired search criteria in a rapidly-accessible form. Indexes are **not**
authoritative stores of the blob data, and should not store entire blob
contents.

Since indexes are not intended to be durable, it is fine to delete and rebuild
them at any time. Indexes can be treated as a type of blobstore which does not
support the `get` operation.

## Implementations

Indexes can be implemented on many kinds of databases. Early support will
probably consist of an in-memory implementation and later a SQLite3-backed
index.

## System Indexes

Vault implements a several types of indexes to speed up data queries. Some of
them are straight from Datomic, to handle sorted datom access.

Unsolved use-cases:
* What entities are owned by a key?

### Blob stats

This is the most basic index; it stores data about the blobs which have been
seen by the indexer.

```clojure
{:blob :ref         ; blob hash-id
 :size :long        ; blob byte length
 :type :keyword     ; data type
 :stored-at :inst}  ; time added to index

[blob]
[stored-at]
[type stored-at]
```

This enables fast lookups for queries like, "Which blobs have been indexed
already, and when?" and, "Which blobs have a given type?" which is needed to
find entity root and update blobs.

### EAVT

The EAVT index provides efficient access to everything about a given entity.
Conceptually this is very similar to row access style in a SQL database.

```clojure
[entity attribute value]
```

### AEVT

The AEVT index provides efficient access to all values for a given attribute,
comparable to traditional column acess style.

```clojure
[attribute entity value]
```

### AVET

The AVET index provides efficient access to particular combinations of attribute
and value. This index only contains datoms for attributes which are marked
`:db/unique` or `:db/index` in some schema location. (Where?)

```clojure
[attribute value entity]
```

The major open question here is where 'indexed attributes' are defined since
there's no storage-wide schema.

### VAET

The VAET index contains all datoms with references to other blobs as the
attribute's value. This enables efficient navigation of relationships in
reverse.

```clojure
[value attribute entity]
```

Ideally, this should support non-entity references too. E.g., if entity **A**
has an attribute _x_ which refers to a tree of data blobs, one of which refers
ultimately to entity **B**, then there should be a way to find out "what points
to **B**" even if the source is not an entity.

### Full-text

The full-text index provides a way to efficiently search for matches in text
data. This may be in entities specifically or in blobs.
