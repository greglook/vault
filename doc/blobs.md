# Blob Storage

One of the core pieces of the system is the storage of 'blobs', identified by
the cryptographic hash of their contents. This scheme is known as _content
addressable storage_: a given hash identifies an immutable piece of data, as a
change in the content results in a change in the identifier. This has several
useful properties:
- Data is immutable, so there's no concern over having the 'latest version' of
  something - you either have it, or you don't.
- Synchronizing data between stores only requires enumerating the stored blobs
  in each and exchanging missing ones.
- Content can be structurally shared by different higher-level constructs. For
  example, a file's contents can be referenced by different versions of
  metadata without duplicating the file data.

## Hash Identifiers

Blobs are referenced by URN-like strings which specify the _cryptographic
digest_ of the blob's contents. This can be represented a few different ways,
with varying levels of verbosity.  The most succinct is to just prepend the
shortened code for the algorithm which produced the hash. Other components could
include the leading 'urn' scheme, and a fully-specified version could use the
'hash' URN namespace. For example, the SHA-256 algorithm hashes the string
"foobarbaz" to the following digest:

<pre>
sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d
urn:sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d
urn:hash:sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d
</pre>

These strings are _hash identifiers_; more generally, the pairing of an algorithm
and hex digest is known as a _blobref_. In practice, the shorter _algo:digest_
form will probably be used internally for brevity. External representations of
the identifier can add the 'urn' components as desired.

## Storage Interface

Blobs are stored as immutable byte sequences with some optional associated
_status_ metadata. The only way to reference a blob is by its hash identifier.

The blob storage interface is straightforward:
- `list` - enumerate the stored blobs
- `stat` - get metadata about a stored blob
- `open` - return a stream of bytes stored for a blob
- `store!` - store a stream of bytes for a blob
- `remove!` - drop a blob from the store

Status metadata is a simple map of information about the stored blob. The
metadata present is largely implementation-specific, but may include some
common information:
- `:size` - the number of bytes stored for the blob
- `:stored-at` - time the blob was added to the store
- `:location` - an optional URI giving a path to the stored resource

An example status map from a blob stored in S3 might look like:

```clojure
{:created-at #inst "2013-12-01T18:23:48Z",
 :location #uri "s3://user-storage/vault/data/sha256/53e/0b9/f7503729f698174615666322f00f916cceb4518e8e1c6f373e53b56180",
 :size 123}
```

## Implementations

Because blob storage is so simple, there are many possibilities for
implementation. Here's some ideas:
- `memory`: transient in-memory blob storage
- `file`: local filesystem blob storage
- `sftp`: store blobs on a remote host accessible via ssh
- `s3`: persist blobs in Amazon's cloud storage service

'Meta-stores' can also wrap multiple blob stores to give more complex storage
systems:
- `replicate`: store blobs in multiple locations
- `cache`: keep a fixed size of local blobs, deferring to another authoritative store

Blob stores can also be composed with _filter_ layers. Some ideas:
- `compress`: compress blobs to save space
- `encrypt`: encrypt blobs, perhaps stored in untrusted third-party services

One issue with transforming filters like this is that the bytes stored in a
location no longer match the hash identifier they are stored under. This may not
be a problem as long as you ensure correctness at some higher level; for
example, you could run a file store on top of an encrypted volume. Alternately,
you can use two stores with one containing the metadata for the other.
