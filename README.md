Vault
=====

[![Build Status](https://travis-ci.org/greglook/vault.svg?branch=develop)](https://travis-ci.org/greglook/vault)
[![Coverage Status](https://coveralls.io/repos/greglook/vault/badge.png?branch=develop)](https://coveralls.io/r/greglook/vault?branch=develop)
[![Dependency Status](https://www.versioneye.com/user/projects/53718feb14c158ff7b00007c/badge.png)](https://www.versioneye.com/user/projects/53718feb14c158ff7b00007c)

Vault is a content-addressable, version-controlled, hypermedia datastore which
provides strong assertions about the integrity and provenance of stored data.
Read on for a general introduction, or see the docs for more detailed
explanations of the various concepts.

This is heavily inspired by both [Camlistore](http://camlistore.org/) and
[Datomic](http://www.datomic.com/). Vault does not aim to be (directly)
compatible with either, though many of the ideas are similar. Why use a new data
storage system? See [some comparisons to other systems](doc/vs.md).

## System Layers

This is a quick tour of the concepts in Vault. For more details, follow the
links or browse around the [doc](doc/) folder.

### Blob Storage

At the lowest level, Vault is built on [content-addressable
storage](doc/blobs.md). Data is stored in _blobs_, which are opaque byte
sequences addressed by a cryptographic hash of their contents. The combination
of a hash algorithm and the corresponding digest is enough information to
securely and uniquely identify a blob. These _hash-ids_ are formatted like a
URN:

`sha256:2f72cc11a6fcd0271ecef8c61056ee1eb1243be3805bf9a9df98f92f7636b05c`

A _blob store_ is a system which can store and retrieve blob data. Blob stores
support a very simple interface; they must store, retrieve, and enumerate the
contained blobs. The simplest type of blob storage is a hash map in memory.
Another simple example is a file system, where blobs are stored as local files.

### Data Format

To represent [structured data](doc/data-structures.md), Vault uses
[EDN](https://github.com/edn-format/edn). Data blobs are recognized by a magic
header sequence: `#vault/data\n`. This has the advantage of still being a legal
EDN tag, though it is stripped in practice.

An example data blob representing a file might look like this:

```clojure
#vault/data
{:change-time #inst "2013-10-23T20:06:13.000-00:00"
 :content #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :group "users"
 :group-id 500
 :modify-time #inst "2013-10-25T09:13:24.000-00:00"
 :name "foo.clj"
 :owner "greglook"
 :owner-id 1000
 :permissions "0755"
 :vault/type :fs/file}
```

Blob references through hash-ids provide a secure way to link to immutable data,
so it is simple to build data structures which automatically deduplicate shared
data. These are similar to Clojure's persistent collections; see the schema for
[hierarchical byte sequences](doc/schema/bytes.edn) for an example.

Certain PGP objects can also be stored in Vault as a recognized data type. The
primary case is storing public key blobs. Keys should be encoded as armored
ascii text blobs.

### Indexing

A second fundamental component of the system is a set of
[indexes](doc/indexing.md) of the data stored in Vault. Indexes can be thought
of as a sorted list of tuples. Different indexes will store different subsets of
the blob data.

### Entities and State

Mutable data is represented in Vault by [entities](doc/entities.md).
- A _root blob_ serves as the static identifier of an entity.
- An _attribute_ is an entity property which is associated with one or more values.
- _Update blobs_ transactionally modify entities' attributes.

Identity and ownership in Vault are handled by [cryptographic
signatures](doc/signatures.md). These provide trust to data that is present in
the blob layer. Signatures are provided as secondary values in a data blob,
following the primary value:

```clojure
{:key #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :signature #pgp/signature #bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMrV61su7iGPQsdXvZqBb2LKUhGku6ZQxqBYOvDdXaTmYIZJBY0CtAOlTe3NXn0kvnTuaPoA6fe6Ji1mndYUudKPpWWld9vzxIYpqnxL/ZtjgjWqkDf02q7M8ogSZ7dp09D1+P5mNnS4UOBTgpQuBNPWzoQ84QP/N0TaDMYYCyMuZaSsjZsSjZ0CcCm3GMIfTCkrkaBXOIMsHk4eddb3V7cswMGUjLY72k/NKhRQzmt5N/4jw/kI5gl1sN9+RSdp9caYkAumc1see44fJ1m+nOPfF8G79bpCQTKklnMhgdTOMJsCLZPdOuLxyxDJ2yte1lHKN/nlAOZiHFX4WXr0eYXV7NqjH4adA5LN0tkC5yMg86IRIY9B3QpkDPr5oQhlzfQZ+iAHX1MyfmhQCp8kmWiVsX8x/mZBLS0kHq6dJs//C1DoWEmvwyP7iIEPwEYFwMNQinOedu6ys0hQE0AN68WH9RgTfubKqRxeDi4+peNmg2jX/ws39C5YyaeJW7tO+1TslKhgoQFa61Ke9lMkcakHZeldZMaKu4Vg19OLAMFSiVBvmijZKuANJgmddpw0qr+hwAhVJBflB/txq8DylHvJJdyoezHTpRnPzkCSbNyalOxEtFZ8k6KX3i+JTYgpc2FLrn1Fa0zLGac7dIb88MMV8+Wt4H2d1c"
 :vault/type :vault/signature}
```

### Application

At the top level, applications are built on top of the data layer. An
application defines semantics for a set of data types. Some example usages:
- Snapshot filesystems for backup, taking advantage of deduplicated blobs to
  store only incremental changes.
- Archive messages such as email, chat, and social media.
- Store and flexibly organize media such as music and photos.
- Maintain personal time-series data for Quantified Self tracking.

One significant advantage of building on a common data layer is the ability to
draw relations between many different kinds of data. Information from a variety
of systems can be correlated into more meaningful, higher-level aggregates.

## Usage

To get started working with Vault, the command-line tool is the simplest
interface. After initializing some basic configuration, you can use the tool to
explore the contents of the blob store. Use `-h` `--help` or `help` to show
usage information for any command. General usage is similar to git, with nested
subcommands for various types of actions.

See the [usage docs](doc/tool.md) for more information.

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
