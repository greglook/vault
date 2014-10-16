Blob Storage
============

A _blob_ is a sequence of bytes identiied by the cryptographic hash of its
contents. A _blob store_ is a system which can store and retrieve blobs by their
hash identifiers. This scheme is known as _content addressable storage_; a given
hash value identifies a specific, immutable piece of data, as a change in the
content results in a change in the identifier.

Vault uses SHA-256 to generate hashes, though in principle any hashing algorithm
could be used. However, 2^256 identifiers is more than enough to identify every
atom in the solar system, which should be sufficient for the forseeable future.

## Hash Identifiers

Blobs are referenced by URN-like strings which specify the cryptographic digest
of the blob's contents. The pairing of an algorithm and hex digest is a _hash
identifier_, or hash-id.

These ids can be represented a few different ways, with varying levels of
verbosity. The most succinct is to just prepend the shortened code for the
algorithm which produced the hash. Other components may include the leading
'urn' scheme, and a fully-specified version could use the 'hash' URN namespace.

For example, the SHA-256 algorithm hashes the string "foobarbaz" to the
following (equivalent) ids:

```
urn:hash:sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d
urn:sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d
sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d
```

In practice, the shorter `algo:digest` form is used for brevity. External
representations of the identifier can add the 'urn' components as desired.

Another non-canonical syntax trades the colon (:) for a hyphen (-) to make the
identifiers path-safe. This lets them be used in URLs and file paths.

## Storage Interface

Blobs are stored as read-only byte sequences with some optional associated
status metadata. The only way to reference a blob is by its hash identifier.
Blob data is represented as a Clojure record with id and content keys:

```clojure
{:id #vault/ref "sha256:9e663220c60fb814a09f4dc1ecb28222eaf2d647174e60554272395bf776495a"
 :content #bin "iJwEAAECAAYFAlNMwWMACgkQkjscHEOSMYqORwQAnfJw0AX/6zabotV6yf2LbuwwJ6Mr+..."}
```

The blob storage interface is straightforward:
- `list` - enumerate the stored blobs
- `stat` - get metadata about a stored blob
- `get` - return the bytes stored for a blob
- `put!` - store a some bytes as a blob
- `delete!` - remove a blob from the store

Status metadata is attached as additional keys in the blob record under the
`stat` namespace. The data present is largely implementation-specific, but may
include some common keys:
- `:stat/size` - the number of bytes stored for the blob
- `:stat/stored-at` - time the blob was added to the store
- `:stat/origin` - URL giving the location of the stored content

A `stat` call is similar to an HTTP HEAD request, in that it returns the blob
with no content. An example from a blob stored in S3 might look like:

```clojure
{:id #vault/ref "sha256:53e0b9f7503729f698174615666322f00f916cceb4518e8e1c6f373e53b56180"
 :stat/origin #uri "s3://user-bucket/vault/sha256/53e/0b9/sha256-53e0b9f7503729f698..."
 :stat/size 12345
 :stat/stored-at #inst "2013-12-01T18:23:48Z"}
```

## Advantages

Content-addressable storage has several useful properties:
- Data references are separated from the knowledge of where and how the data is
  stored.
- Data is immutable, so there's no concern over having the 'latest version' of
  something - you either have it, or you don't.
- Synchronizing data between stores only requires enumerating the stored blobs
  in each and exchanging missing ones.
- Content can be structurally shared by different higher-level constructs. For
  example, a file's contents can be referenced by different versions of
  metadata without duplicating the file data.

## Implementations

Because blob storage is so simple, there are many possibilities for
implementation:
- `memory`: transient in-memory blob storage
- `file`: local filesystem blob storage
- `sftp`: store blobs on a remote host accessible via ssh
- `s3`: persist blobs in Amazon's cloud storage service

'Meta-stores' can also wrap multiple blob stores to give more complex storage
systems:
- `aggregate`: search multiple blob stores in order for blobs
- `replicate`: copy blobs to multiple stores
- `cache`: keep a fixed size of blobs locally, deferring to another authoritative store

## The Encoding Problem

One goal of the system is to provide secure and efficient blob storage. Many
blobs will be amenable to compression to reduce storage and bandwidth costs. In
other cases, blobs will be stored in third-party systems not directly under the
user's control, and the blob contents should be encrypted for security.

Both of these represent _encodings_ of the blob content. One way to provide this
functionality would be another higher-order blob store which wraps another
'backing' store. Blob contents would be encoded on the way in, and decoded on
the way out. The problem with this approach is that it is not composable - it
violates the assumption that the identifier for some stored data matches the
actual hash of the content.

One way around this is to provide the encoding at a lower level. For example, a
normal file-based store can be run on top of an encrypted block device to
provide encryption at rest for the stored data. However, this is not always
possible for remote storage systems.

Another issue is metadata - the blob storage interface explicitly avoids
providing any mechanism for storing extra metadata with a blob. If a naive
encoding store as described above is used to gzip blobs being stored as local
files, and the user decides to switch to bzip instead, it breaks the store.
Either the code has to guess which algorithm a given blob is compressed with, or
the entire data store needs to be re-compressed with the new algorithm. A
similar issue exists with algorithms, keys, and IVs for encryption stores.

The best design to handle this so far is a higher-order store that uses _two_
backing stores. One stores the data blobs, which have the correct hash-id for
the encoded content. The other stores metadata blobs, which record a mapping
from the original hash-ids to the identifiers for the encoded blobs. This
metadata store can also keep any additional data necessary to decode the blobs.

On startup, the store scans everything in the metadata blob store to build the
mapping. For performance, it would be a good idea to occasionally compact the
metadata into fewer, larger blobs to reduce load times.
