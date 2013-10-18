# Vault

A Clojure library and application to store documents in a content-addressable
datastore while maintaining a secure history of entity values.

## Notes

In general, try to think of CAS as a durable version of Clojure's
persistent data structures. Since blobs are immutable once stored and can be
structurally shared among multiple dat astructures, you get many of the same
benefits; an address to a certain value will _always_ resolve to the same value
since things are addressed by the hash of their contents.

Permanodes let you assign a continuous identity to a sequence of these values,
much like Clojure's reference types (namely, atoms). Thus, permanodes represent
_entities_ and track the current state of that entity over time. Claims which
modify a permanode should include a reference to the 'prior' permanode state.
This gives version-control semantics and lets branches and merges happen.

Considering this, there are two classes of reference to other blobs. Either the
data structure is _including_ the referenced blob as an extension of the data,
or it is _linking_ to an entity represented by a permanode.

A data structure should be split into multiple blobs if a part of the data is
very large and will change less frequently than other parts of the data, or
equivalently, is likely to be shared frequently. As an example, the metadata
representing a filesystem directory includes metadata like the directory name,
permissions, access times, and so on. It also contains a set of children, which
may be quite large. If the set of children is included directly in the
directory metadata, then operations such as renaming cause the entire set to be
stored again, even though it didn't change. A better practice would be to
make the children attribute of the structure a **blobref** to a separately
stored static set of the children.

On the other hand, when the data includes a reference to an _identity_, the
data should refer to a permanode representing that entity. As an example, a
financial transaction includes `to` and `from` attributes. The simplest case
would be to use a string designating the entities, but this does not scale well
if more information about the entity should be stored. An alternative is to
refer to a blob of metadata about the entity, but this is brittle - if the
metadata changes, all the financial transactions must be re-stored with an
updated hash reference. The better design is to store transactions which refer
to the permanode representing the entities in question, which can then be
updated at will. If necessary, the state of the entity at an arbitrary time can
be recovered.

Permanodes can store metadata about the values they represent. Some simple ideas
are `title`, `description`, etc. Can also include type/schema information, like
registering custom unit equivalents for physical quantities in another blob and
referring to it from the permanode attributes.

Another desired feature of the system is an organizational layer on top of the
permanode entities. In effect, one or more classification heirarchies. In
practice, each group/class will be a permanode representing a dynamic set. Each
group contains references to permanodes of that class and potentially other
subgroup sets.

### Indexing questions
The following are use-cases for specific indices on the data. Still need to
understand Camlistore's indexing system better.

* What permanodes are owned by a key?
* What permanodes have been affected by claims made by a key?
* What claims have been made about a permanode over time?
* What claims have been made about a specific attribute of a permanode over time?

Could handle this with a Claims table:
  string        claim-ref
  int           subclaim
  timestamp     time
  string        key-id
  string        permanode-ref
  string        type
  string        attr
  string        value

Primary key:
    [claim-ref subclaim]
Indices:
    [key-id time]
    [permanode-ref key-id time] (not convinced this is ideal)
    [permanode-ref attr time]

## License

Copyright © 2013 Gregory Look
