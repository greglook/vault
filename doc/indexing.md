Data Indexing
=============

In order to access stored data efficiently, it must be searchable. While it is
possible to exhaustively scan the entire data store and calculate the relevant
information each time a query is performed, a much better aproach is to _index_
the data.

In general, an index is a _view_ of the stored data which caches desired search
criteria in a rapidly-accessible form. Indexes are **not** authoritative stores
of the blob data, and should not store entire blob contents.

Since indexes are not intended to be durable, they can be destroyed and rebuilt
at any time.

## Corpus

The various indexes on the blobs in a user's store are arranged into a _corpus_.
The corpus supports most of the methods of a blob-store except the `get`
operation. To index a blob, you simply `put!` it into the corpus.

The corpus uses one of the contained indexes to determine whether it's already
seen a blob. See the [blob index](#Blob%20Index) below.

(name to be improved)

## Search Interface

Each index contains some _records_ of data and provides quick lookups based on
values of those attributes. Lookups can be optimized by creating sorted value
sequences for different query types.

As an example, if index `foo` stores the following type of data, and sets up
the `:foo/id` and `:foo/alpha` sequences:

```clojure
{:id    String
 :alpha Long
 :beta  Long}

:foo/id    [id]
:foo/alpha [alpha beta]
```

- A query specifying the `id` would be looked up in the `:foo/id` index.
- A query by `alpha` value would use the `:foo/alpha` index.
- A query for `beta` would result in a slow scan of `:foo/alpha`.
- A query for `id` and `alpha` would be looked up in `:foo/id`, because it
  fully specifies the index. The resulting values would be filtered by `alpha`.

```clojure
(index/search foo {:id "abcdefg"})
; =>
({:id "abcdefg"
  :alpha 123
  :beta 456})
```

This is basic query optimization, commonly done in databases using indexes
(unfortunate name collision). It should also be possible to return results in
reverse order at no additional cost.

### Low-Level Indexes

The first two indexes deal with blobs and references, mapping the nodes and
edges of the blob graph.

### Blob Index

This is the most basic index; it stores data about the blobs which have been
seen by the indexer.

```clojure
{:blob      HashID      ; blob hash-id
 :size      Long        ; blob byte length
 :type      Keyword     ; data type
 :label     String      ; type-specific annotation
 :stored-at DateTime}   ; time added to index

:blob/id   [blob]
:blob/type [type label]
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
{:blob HashID     ; source hash-id
 :type Keyword    ; source blob type
 :ref  HashID     ; target hash-id
 :time DateTime}  ; source blob stored-at

:ref/from [blob]
:ref/to   [ref type time]
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
 :value     ???         ; serialized EDN value
 :tx        HashID      ; root or update blob hash-id
 :time      DateTime}   ; assertion timestamp from blob

:datom/log  [time tx]                             ; history index
:datom/eavt [entity attribute value time tx op]   ; row index
:datom/aevt [attribute entity value time tx op]   ; column index
:datom/avet [attribute value entity time tx op]   ; value index
:datom/vaet [value attribute entity time tx op]   ; reverse index
```

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

## Implementations

Indexes can be implemented on many kinds of databases. Early support will
probably consist of an in-memory implementation and later a SQLite3-backed
index.
