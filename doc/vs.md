Comparison to Other Systems
===========================

Vault obviously isn't the only storage system in town, so why use it over any
of the existing ones?

## Filesystems

The advantage over a file system tree should be obvious, but for completion:
- Flat files generally have no defined structural syntax.
- Addressing is ad-hoc and location-based, and typically varies across
  subsystems.
- Files can't be moved without breaking references in other files.
- Files are mutable and not versioned.
- Changes to files are not atomic.
- Files are not indexed.

## Relational Databases

Databases are a popular solution for data storage. They solve (to some degree)
the problem of location-based addressing, and provide excellent indexing of the
data. In addition, data representation is made explicit by the database schema.
While good systems can be built on top of them, there are still a number of
drawbacks:
- Databases rows are mutable and not versioned.
- Database schemas are inflexible, restricting what kind of data can be stored.
- Harder to represent general entities with many aspects.
- References are type-restricted by foreign key constraints.
- Databases (typically) do not handle binary data well.

## Version Control Systems

Git actually comes very close to matching the capabilities of Vault. The main
downside is that Git is designed primarily to work with text, and performs
poorly when handling large binary data. There have been some projects like
`git-annex` which aim to solve this, but they are a little clunky.

Additionally, version control systems are generally designed to allow multiple
"world lines" (branches) to exist and support splitting and merging them. For a
data store, this is generally overkill. Note that such semantics can still be
built on top of Vault, it just doesn't include them in the base entity model.

VCS options generally solve the mutability and versioning problems, but fail to
address references and indexing. In addition, there's still no standard syntax
for structured data.

## Datomic

Vault is heavily inspired by Datomic, so why isn't Datomic good enough as-is?
The difference stems mainly from design goals; Datomic is targeting real-time
execution of complex datalog queries in a performance-sensitive context. Vault
is targeted at maximum flexibility in data modeling instead.

One of Datomic's tradeoffs is that it requires a schema for attributes. This is
much more flexible than a traditional database schema, but still requires
declaration.

In addition, in Datomic every piece of data must be an entity, and the only
references are to other entities. It is not possible (for example) to create an
immutable snapshot of a directory and its files. Each directory node would need
to be an entity itself, and therefore capable of being changed. You could
recover what the tree _looked like_ at any point in time, because Datomic is
awesome like that, but it wouldn't be an immutable value you could reference.
Datomic also doesn't handle binary data well, so it falls somewhat short on the
filesystem snapshot front anyway.

## Camlistore

TODO: discuss
