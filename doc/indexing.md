Data Indexing
=============

The information in Vault is indexed to enable efficient access. At a basic
level, an index stores a certain type of records and provides an efficient way
to search them by querying.

Indexes are **not** authoritative stores of the blob data, and should not store
entire blob contents. Since index state is not intended to be durable, it can be
destroyed and rebuilt at any time from the source blob data.

## Index Definition

An index is defined by three properties:
- A _record schema_ which specifies the attributes being indexed.
- A _projection function_ which transforms a blob into a sequence of records.
- A set of _query types_ which name the attributes used to search the index.

Blobs may map to zero records, indicating that the blob is not relevant to the
index. Alternately, a single blob can map to many records, as in the case of
datoms from an entity update blob.

## Implementations

An index is implemented by an underlying system which handles the record storage
and querying. For example, a brute-force index could be implemented with a
backing blob store and the projection function alone. To answer a query, the
entire blob store would be enumerated, each blob transformed into records, and
the records matched against the given pattern.

Indexes can be implemented on many kinds of databases. Early support will
consist of an in-memory index and later a SQLite3-backed index.

For ideas on later implementations which could use a blob store as backing data,
see [this blog
post](http://tonsky.me/blog/unofficial-guide-to-datomic-internals/) about
Datomic's internal index data structure.

## Catalog

The various indexes on the blobs in a user's store are collected into a
_catalog_. The catalog supports most of the methods of a blob-store except
the `get` operation. To index a blob, you simply `put!` it into the catalog.

The catalog uses one of the contained indexes to determine whether it's
already seen a blob. See the [blob stats index](#blob-stats) below.

## Graph Indexes

The first two indexes deal with blobs and references, mapping the nodes and
edges of the blob graph.

### Blob Stats

This is the most basic index; it stores records about the blobs which have been
indexed. This lets the catalog implement the `enumerate` and `stat` blob-store
operations.

```clojure
{:blob      HashID      ; blob hash-id (pk)
 :size      Long        ; blob byte length
 :type      Keyword     ; data type
 :label     String      ; type-specific annotation
 :stored-at DateTime}   ; time added to index

:direct [blob]          ; direct lookups
:typed  [type label]    ; blobs by type/label
```

Use cases:
- Implementing blob store `enumerate` and `stat` operations.
- Search for arbitrary blobs by data type.
- Looking up PGP keys by storing the hexadecimal key identifier as the blob
  label.

### Data Links

This index stores records about hash-id references between blobs, giving quick
access forwards and backwards.

```clojure
{:blob HashID         ; source hash-id
 :type Keyword        ; source blob type
 :ref  HashID}        ; target hash-id

:forward [blob]       ; forward lookups
:reverse [ref type]   ; reverse lookups
```

Use cases:
- Find arbitrary blobs (by type) which reference a given blob.
- Find references without re-parsing the source blob.

Slightly more complex use-case: find the mutations made to the data store by a
certain identity.
1. Look up the public key hash-id in the ref index.
2. Query for `:vault.entity/root` and `:vault.entity/update` blobs which
   reference the public key.
3. Filter blobs by checking the public keys used in their signatures.

## Entity Indexes

Entity indexes address the higher-level data structures in Vault. These are
adaptations from Datomic, and store _datoms_, which are atomic data assertions.

```clojure
{:op        Keyword     ; datom operation (:attr/set, :attr/add, etc)
 :entity    HashID      ; entity root hash-id
 :attribute Keyword     ; attribute keyword
 :value     *           ; serialized EDN value
 :tx        HashID      ; root or update blob hash-id
 :time      DateTime}   ; assertion timestamp from blob

:log  [time tx]                             ; history index
:eavt [entity attribute value time tx op]   ; row index
:aevt [attribute entity value time tx op]   ; column index
:avet [attribute value entity time tx op]   ; value index
:vaet [value attribute entity time tx op]   ; reverse index
```

These are presented to the system as a single index which can be searched for
datoms, but internally the query will be mapped to whichever sub-index is most
efficient.

The datom index is configured with the _attribute schema_, which specifies which
attributes should be part of the AVET index. Other uses for this schema TBD.

### Transaction Log

The transaction log provides a history of datoms over time, grouped by
transaction blob.  This is a little different than the other types in that it is
not a _covering_ index. Instead, it's just a time-sorted list of the entity root
and update blobs, which are converted into a datom sequence as needed.

### EAVT

The EAVT index provides efficient access to everything about a given entity.
Conceptually this is very similar to row access style in a SQL database.

### AEVT

The AEVT index provides efficient access to all values for a given attribute,
comparable to traditional column acess style.

### AVET

The AVET index provides efficient access to particular combinations of attribute
and value. This index only contains datoms for attributes which are marked
`:db/unique` or `:db/index` in some schema definition.

The major open question is where 'indexed attributes' are defined since there's
no storage-wide schema.

### VAET

The VAET index contains all datoms with references to other blobs as the
attribute's value. This enables efficient navigation of relationships in
reverse.

## Full-text Index

The full-text index provides a way to efficiently search for matches in text
data. How blobs are selected to be stored in the full-text index is still to be
determined. Presumably the index only covers latest-value of selected entity
attributes.
