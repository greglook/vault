Application Programming Interface
=================================

The top layer is a set of interfaces for building applications which store data
in Vault. This gives the apps access to:
- scalable, unified content storage
- persistent structured data
- automatic content versioning
- cryptographically verifiable audit trail

## Use Cases

Brainstorming some uses here. As an application, I want to...

### Store and Retrieve Binary Data

Applications should be able to feed an arbitrarily large stream of bytes into
Vault and get back a hash-id reference. The stream should automatically be
chunked with a rolling checksum and constructed into a byte-seq data structure.

On the flip side, there should be a common "feed me a stream of bytes referenced
by this hash-id" function. This should treat `:raw` blobs as direct content
sources, and turn `:byte/seq` structures into a continuous lazily-fetched byte
stream.

### Store Data Blobs

The data layer should accept a data structure and create an appropriately
labeled and sorted data blob, returning the resulting HashID. Additionally, a
convenience would be marking blobs to separate out with some EDN tag to
automatically resolve blob dependencies.

### Store Entity Data

Another common use-case is creating, updating, and deleting entities to store
data. The application should not need to worry about key management or signature
details. Apps will want to:
- create an entity with some id and initial state
- apply updates to entities
- delete entities (tombstones?)
- get the current state of an entity (EAVT)
- get the historical state of an entity (EAVT)
- list the changes to an entity over time (tx-log)
- find all entities with a certain attribute (AEVT)
- find entities with a specific value for an attribute (AVET)
- find links to an entity (VAET)
