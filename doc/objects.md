# Identity and State

Immutable data is great, but to be useful the system needs to be able to
represent _mutable state_ somehow. Say a user stores a document; that content
has some address based on its hash digest. If the user updates the document,
they must keep a different address for the new version. The concept tying the
different versions of the document together is _identity_.

Data _objects_ assign a continuous identity to a sequence of values. Thus,
objects can be used to represent entities and track the 'current state' of
that entity over time. Objects are referenced by the address of their
_root_.

Object roots are static, so changes to that object are enacted by adding
further _change blobs_ to the store. These blobs specify accumulating
modifications to the object, thus the 'current' state of the object can be
determined by applying the full sequence of changes made to it.

Changes should include a reference to the 'prior' object change. This gives
version-control semantics and lets branches and merges happen.

## Object Attributes

Objects act much like a map, containing various named attributes. These mutable
properties are specified directly by change blobs, and generally support
_metadata_ about the entity represented by the object. Some common attributes:
- `title`: string naming the object
- `description`: longer string describing the object
- `content-type`: MIME type for the object's content
- `tags`: set of string tags labeling the object

Objects can also have a _value_, which is considered to be the 'state' of the
object. Whether to assign a value to an object or use the attributes depends on
the nature of the data.

## Resolving State

Since each change blob references its 'parent' changes, these blobs form a tree
descending from the object root. The 'current' state of the object is the tip
of the tree with the latest timestamp.

TODO: discuss conflict resolution?

## Example

When data includes a reference to an _identity_, the attribute should refer to a
object representing that entity. As an example, a financial transaction may
include `to` and `from` attributes. The simplest case would be to use a string
designating the entities, but this does not scale well if more information about
the entity should be stored. An alternative is to refer to a blob of metadata
about the entity, but this is brittle - if the metadata changes, all the
transactions must be re-stored with an updated hash reference. The better design
is to store transactions which refer to the object representing the entities
in question, which can then be updated at will. If necessary, the state of the
entity at an arbitrary time can be recovered by replaying the changes up to
then.
