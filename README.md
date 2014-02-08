Vault
=====

Vault is a Clojure library and application to store documents in a
content-addressable datastore while maintaining a secure history of entity
values. See the docs for more detailed explanations of the various pieces.

This is heavily inspired by both [Camlistore](http://camlistore.org/) and
[Datomic](http://www.datomic.com/). Vault does not aim to be (directly)
compatible with either, though many of the concepts are similar.

## Concepts

This is a rough outline of the concepts developing in Vault. For more details,
follow the links or browse around the [doc](doc/) folder.

### Blob Layer

At the lowest level, vault is built on [content-addressable
storage](doc/blobs.md). Data is stored in _blobs_, which are some sequence of
bytes addressed by a secure hash of their contents. The combination of a hash
algorithm and the corresponding digest is enough information to securely and
uniquely identify a blob. These _blobrefs_ are formatted like a URN:

`sha256:2f72cc11a6fcd0271ecef8c61056ee1eb1243be3805bf9a9df98f92f7636b05c`

A _blob store_ is a system which can store and retrieve blob data by address.
Blobs can be _encoded_ before being stored, performing operations like
compression and encryption.

### Data Layer

The [data layer](doc/data-structures.md) is built on the blob storage layer.
Vault data is stored as [EDN](https://github.com/edn-format/edn) in UTF-8 text.
It is recognized by a magic header sequence: `#vault/data\n`. This has the
advantage of still being a legal EDN tag, though it is stripped in practice.

Blob references provide a secure way to link to immutable data, so it is simple
to build data structures which automatically deduplicate shared data. These are
similar to Clojure's persistent collections; see the schema for [hierarchical
byte sequences](doc/schema/bytes.edn) for an example.

In order to represent mutable entities, vault uses the idea of
[entities](doc/entities.md).
- A _root blob_ serves as the static identifier of an entity.
- An _attribute_ is an entity property which is associated with one or more values.
- An _update blob_ modifies entities' attributes at some point in time.

Identity and ownership in vault is provided by [cryptographic
signatures](doc/signatures.md). These provide trust to data that is present in
the blob layer.

Finally, the data layer implements efficient querying by
[indexing](doc/indexing.md) entities and their attributes.

### Application Layer

At the top level, applications are built on top of the data layer. An
application defines semantics for a set of data types. Some example usages:
- Snapshot filesystems for backup, taking advantage of deduplicated blobs to
  store only incremental changes.
- Archive messages such as email, chat, and social media.
- Store and flexibly organize media such as music and photos.
- Maintain personal time-series data for Quantified Self tracking.

One significant advantage of building on a comman data layer is the ability to
draw relations between many different kinds of data. Information from a variety
of systems can be correlated into more meaningful, higher-level aggregates.

## Usage

To get started working with vault, the command-line tool is the simplest
interface. After initializing some basic configuration, you can use the tool to
explore the contents of the blob store. Use `-h` `--help` or `help` to show
usage information for any command. General usage is similar to git, with nested
subcommands for various types of actions.

See the [usage docs](doc/tool.md) for more information.

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
