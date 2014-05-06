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

## Graph Indexes

The first two indexes are on the _nodes_ and _edges_ formed by the blob graph.

### Node Index

This is the most basic index; it stores data about the blobs which have been
seen by the indexer.

```clojure
{:blob      HashID      ; blob hash-id
 :size      Long        ; blob byte length
 :type      Keyword     ; data type
 :stored-at DateTime}   ; time added to index

:blob/id   [blob]
:blob/time [stored-at]
:blob/type [type stored-at]
```

This enables fast lookups for queries like, "Which blobs have been indexed
already, and when?" and, "Which blobs have a given type?" which is needed to
find entity root and update blobs.

### Edge Index

This index stores the references between blobs, giving quick access forwards
and backwards.

```clojure
{:source HashID     ; source hash-id
 :target HashID     ; target hash-id
 :type   Keyword}   ; source blob type

:ref/from [source]
:ref/to   [target type source]
```

## Entity Indexes

The log index is simply a time-series of the entity root, update, and deletion
blobs.

```clojure
{:owner  HashID     ; public key hash-id
 :entity HashID     ; entity root hash-id
 :tx     HashID     ; root/update/delete blob hash-id
 :type   Keyword    ; blob type
 :time   DateTime}  ; time from blob

:entity/for     [owner entity]
:entity/history [entity time]
:entity/log     [time]
```

## Datom Indexes

These indexes are adaptations from Datomic, and store _datoms_, which are atomic
data assertions. All datom indexes provide records like the following:

```clojure
{:op        Keyword     ; datom operation (:attr/set, :attr/add, etc)
 :entity    HashID      ; entity root hash-id
 :attribute Keyword     ; attribute keyword
 :value     String      ; serialized EDN value
 :tx        HashID      ; root or update blob hash-id
 :time      DateTime}   ; timestamp from root or update blob
```

### EAVT

The EAVT index provides efficient access to everything about a given entity.
Conceptually this is very similar to row access style in a SQL database.

```clojure
:datom/eavt [entity attribute value time]
```

### AEVT

The AEVT index provides efficient access to all values for a given attribute,
comparable to traditional column acess style.

```clojure
:datom/aevt [attribute entity value time]
```

### AVET

The AVET index provides efficient access to particular combinations of attribute
and value. This index only contains datoms for attributes which are marked
`:db/unique` or `:db/index` in some schema definition.

```clojure
:datom/avet [attribute value entity time]
```

The major open question is where 'indexed attributes' are defined since there's
no storage-wide schema.

### VAET

The VAET index contains all datoms with references to other blobs as the
attribute's value. This enables efficient navigation of relationships in
reverse.

```clojure
:datom/vaet [value attribute entity time]
```

This is similar to the edge index, but specific to entities and attributes.
This index is therefore time-sensitive, whereas the edge index stores
references between all blobs.

## Full-text

The full-text index provides a way to efficiently search for matches in text
data. How blobs are selected to be stored in the full-text index is still to be
determined.

## Implementations

Indexes can be implemented on many kinds of databases. Early support will
probably consist of an in-memory implementation and later a SQLite3-backed
index.
