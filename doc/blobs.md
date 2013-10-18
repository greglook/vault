# Blob Storage

One of the core pieces of the system is the storage of 'blobs', addressed by
the cryptographic hash of their contents. This scheme is called _content
addressable storage_: a given hash addresses an immutable piece of data,
as a change in the content results in a change in the address. This has a few
nice properties:
- Data is immutable, so there's no concern over having the 'latest version'
  of something - you either have it, or you don't.
- Synchronizing data between stores only requires enumerating the stored blobs
  in each and exchanging missing ones.
- Blobs can be structurally shared by different higher-level constructs. For
  example, a file's contents can be referenced by different metadata without
  duplicating the file.

## Referring to Blobs

Blobs are referenced by URN-like hash addresses. For example, the string
`"foobarbaz"` has the following hash:

 97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d

This can be represented a few different ways, with varying levels of verbosity.
The most succinct is to just prepend the shortened code for the algorithm which
produced the hash. Other componets could include the leading 'urn' scheme, and
a fully-specified version could use the 'hash' URN namespace:

- sha256:97df358...
- urn:sha256:97df358...
- urn:hash::sha256:97df358...

In practice, the first one will probably be used within the system. External
representations of the data can add the 'urn' scheme as desired.

## Storage Interface

Blob stores only need to support an extremely simple interface:
- `get`: return the bytes comprising a blob
- `put`: store a blob and return the blobref
- `stat`: return the size (and possibly other info) of a stored blob
- `list`: enumerate the stored blobs

The interface should be fairly self explanatory. The `stat` command is mostly
useful as a quick presence check - if information is returned, the store has
the blob in question. Other than blob size, `stat` could also return some
timestamp info, similar to the linux command.

The `list` command should probably return blobrefs paired with their stat info,
to prevent n+1 queries against the blobstore. In general, this info will
probably be available as a byproduct of enumerating the stored blobs anyway.

## Implementations

Because blob storage is so simple, there are many possibilities for
implementation. The most straightforward is _local_ storage, where blobs are
stored as local files on disk. Other possibilities include _sftp_ to files on
a remote host, or in _s3_ or other cloud-storage services.

Blob stores can also be composed with intermediate layers. Some ideas:
- `compress`: compress blobs to save space (may not be effective on all files)
- `encrypt`: encrypt blobs stored in untrusted third-party services
- `shard`: distribute blobs across different stores
- `replicate`: store blobs in multiple locations
- `cache`: preferentially access files but keep a fixed size locally

QUESTION: How does caching square with never deleting blobs?
