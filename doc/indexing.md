Data Indexing
=============

While it is possible to exhaustively scan the entire data store and calculate
the relevant information each time a query is performed, a much better aproach
is to _index_ the data in the blobs.

An index provides a view of the stored data which caches desired blob properties
as rapidly-accessible _records_. Indexes are **not** authoritative stores of the
blob data, and should not store entire blob contents.

Since index state is not intended to be durable, it can be destroyed and rebuilt
at any time from the source blob data.

## Architecture

An index is defined by a _record schema_ and a _projection function_ which
transforms a blob into a sequence of records to store in the index. Blobs may
map to zero records, indicating that the blob is not relevant to the index.
Alternately, a single blob can map to many records, as in the case of datoms
from an entity update blob.

Each index is then implemented by an underlying _search engine_ which handles
the record storage and querying. For example, a 'brute force' engine could be
implemented with a backing blob store and the projection function. To answer a
query, the entire blob store would be enumerated, each blob transformed into
records, and the records matched against the given pattern.

Engines can be implemented on many kinds of databases. Early support will
probably consist of an in-memory implementation and later a SQLite3-backed
engine.

## Search Interface

Each index contains _records_ of data and provides quick lookups based on the
values of the record attributes. To search for matching records, the user
provides a _pattern_ of attributes to match on.

```clojure
; If records in 'foo' look like this:
{:id    String
 :alpha Long
 :beta  Long}

; Searches return a sequence of matching records:
(index/search foo {:alpha 123})
;=>
({:id "abc", :alpha 123, :beta 456}
 {:id "xyz", :alpha 123, :beta 897}
 ...)
```

Ideally, the index should optimize lookups for common patterns. In a relational
database, the records in an index would map to rows in a table, and
optimizations could be made by creating indexes on the relevant columns.

## Corpus

The various indexes on the blobs in a user's store are collected into a
_corpus_ (name to be improved). The corpus supports most of the methods of a
blob-store except the `get` operation. To index a blob, you simply `put!` it
into the corpus.

The corpus uses one of the contained indexes to determine whether it's already
seen a blob. See the [blob index](#blob-index) below.

## Low-Level Indexes

The first two indexes deal with blobs and references, mapping the nodes and
edges of the blob graph.

### Blob Index

This is the most basic index; it stores data about the blobs which have been
indexed. This lets the corpus implement the `enumerate` and `stat` blob-store
operations.

```clojure
{:blob      HashID      ; blob hash-id (pk)
 :size      Long        ; blob byte length
 :type      Keyword     ; data type
 :label     String      ; type-specific annotation
 :stored-at DateTime}   ; time added to index

; Queries:
:direct [blob]          ; direct lookups
:typed  [type label]    ; blobs by type/label
```

Use cases:
- Implementing blob store `enumerate` and `stat` operations.
- Search for arbitrary blobs by data type.
- Looking up PGP keys by storing the hexadecimal key identifier as the blob
  label.

### Ref Index

This index stores the references between blobs, giving quick access forwards
and backwards.

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

#### Log

The log provides a history of datoms over time, grouped by transaction blob.
This is a little different than the other types in that it is not a _covering_
index. Instead, it's just a time-sorted list of the entity root and update
blobs, which are converted into a datom sequence as needed.

#### EAVT

The EAVT index provides efficient access to everything about a given entity.
Conceptually this is very similar to row access style in a SQL database.

#### AEVT

The AEVT index provides efficient access to all values for a given attribute,
comparable to traditional column acess style.

#### AVET

The AVET index provides efficient access to particular combinations of attribute
and value. This index only contains datoms for attributes which are marked
`:db/unique` or `:db/index` in some schema definition.

The major open question is where 'indexed attributes' are defined since there's
no storage-wide schema.

#### VAET

The VAET index contains all datoms with references to other blobs as the
attribute's value. This enables efficient navigation of relationships in
reverse.

## Full-text Index

The full-text index provides a way to efficiently search for matches in text
data. How blobs are selected to be stored in the full-text index is still to be
determined.

```clojure
{:blob HashID
 :text String}

; ...?
```
