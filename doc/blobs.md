# Blob Storage

One of the core pieces of the system is the storage of 'blobs', identified by
the cryptographic hash of their contents. This scheme is known as _content
addressable storage_: a given hash identifies an immutable piece of data, as a
change in the content results in a change in the identifier. This has several
interesting properties:
- Data is immutable, so there's no concern over having the 'latest version' of
  something - you either have it, or you don't.
- Synchronizing data between stores only requires enumerating the stored blobs
  in each and exchanging missing ones.
- Content can be structurally shared by different higher-level constructs. For
  example, a file's contents can be referenced by different versions of
  metadata without duplicating the file data.

## Referring to Blobs

Blobs are referenced by URN-like strings which specify the content's _digest_.
This can be represented a few different ways, with varying levels of verbosity.
The most succinct is to just prepend the shortened code for the algorithm which
produced the hash. Other components could include the leading 'urn' scheme, and
a fully-specified version could use the 'hash' URN namespace. For example, the
SHA-256 algorithm hashes the string "foobarbaz" to the following digest:

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
information present is largely implementation-specific, but may include some
common information:
- `:original-length` - the number of bytes in the raw blob
- `:content-length` - the number of bytes stored for the blob
- `:content-type` - a MIME type for the data in the blob
- `:stored-at` - time the blob was added to the store
- `:location` - an optional URI giving a path to the stored resource
- `:codecs` - an array of codec filters which were applied to the data

The `:codecs` field is especially important to ensure that changes to the blob
store configuration don't break decoding of existing blobs. This should be set
to the sequence of codecs applied, in order. Mostly this will probably be empty
(equivalently, omitted), but suppose a user wanted to compress blobs, then
encrypt them for storage by a third party. In that case, the status metadata
could look like this:

```clojure
{:data-size 123,
 :codecs [:compress/gzip :encrypt/pgp],
 :location #uri "s3://greglook-storage/vault/data/sha256/53e/0b9/f7503729f698174615666322f00f916cceb4518e8e1c6f373e53b56180",
 :stored-at #inst "2013-12-01T18:23:48Z",
 :stored-size 87,
 :mime/content-type "text/plain"}
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
