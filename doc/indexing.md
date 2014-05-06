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

### Edge Index

This index stores the references between blobs, giving quick access forwards
and backwards.

```clojure
{:source :ref       ; source hash-id
 :target :ref       ; target hash-id
 :type :keyword}    ; source blob type

[target type source]
```

## Datom Indexes

These indexes are adaptations from Datomic, and store _datoms_, which are atomic
data assertions. All datom indexes provide records like the following:

```clojure
{:op :keyword           ; datom operation (:attr/set, :attr/add, etc)
 :entity :ref           ; entity root hash-id
 :attribute :keyword    ; attribute keyword
 :value :string         ; serialized EDN value
 :tx :ref               ; root or update blob hash-id
 :time :inst}           ; timestamp from root or update blob
```

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
`:db/unique` or `:db/index` in some schema definition.

```clojure
[attribute value entity]
```

The major open question is where 'indexed attributes' are defined since there's
no storage-wide schema.

### VAET

The VAET index contains all datoms with references to other blobs as the
attribute's value. This enables efficient navigation of relationships in
reverse.

```clojure
[value attribute entity]
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
