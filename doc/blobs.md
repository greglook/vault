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

The interface to a blob store is relatively simple:
- `algorithm`: get the store's configured hashing algorithm
- `enumerate`: list the stored blobs
- `stat`: get metadata about a stored blob
- `open`: return the bytes comprising the blob contents
- `store!`: store byte content and return the blobref
- `remove!`: drop a blob from the store

The interface should be fairly self explanatory. The `stat` command is mostly
useful as a quick presence check - if information is returned, the store has
the blob in question. Other than blob size, `stat` could also return some
other info, such as timestamps, content type, location, etc.

## Implementations

Because blob storage is so simple, there are many possibilities for
implementation. Here's some ideas:
- `memory`: transient in-memory blob storage
- `file`: local filesystem blob storage
- `sftp`: store blobs on a remote host accessible via ssh
- `s3`: persist blobs in cloud storage service

Blob stores can also be composed with intermediate layers. Some ideas:
- `compress`: compress blobs to save space (may not be effective on all files)
- `encrypt`: encrypt blobs (perhaps stored in untrusted third-party services)
- `shard`: distribute blobs across different stores (why would you do this?)
- `replicate`: store blobs in multiple locations (should this just be a backup/sync job?)
- `cache`: keep a fixed size of local blobs, deferring to another authoritative store
