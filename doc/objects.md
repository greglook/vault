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
further _update blobs_ to the store. These blobs specify accumulating
modifications to the object, thus the 'current' state of the object can be
determined by applying the full sequence of updates made to it.

## Motivation

When data includes a reference to an _identity_, the attribute should refer to a
object representing that entity. As an example, a financial transaction may
include `to` and `from` attributes. The simplest case would be to use a string
designating the entities, but this does not scale well if more information about
the entity should be stored. An alternative is to refer to a blob of metadata
about the entity, but this is brittle - if the metadata changes, all the
transactions must be re-stored with an updated hash reference. The better design
is to store transactions which refer to the object representing the entities
in question, which can then be updated at will. If necessary, the state of the
entity at an arbitrary time can be recovered by replaying the updates up to
then.

## Attributes and Content

Objects act much like a map, containing various named attributes. These mutable
properties are specified directly by update blobs, and generally support
_metadata_ about the entity represented by the object. Some common attributes:
- `identity`: a (unique) symbolic shortcut to give to the object
- `content`: the 'state value' of the object
- `title`: string naming the object
- `description`: longer string describing the object
- `tags`: set of string tags labeling the object
- `http/content-type`: MIME type for the object's content

## Causal Ordering

There are two approaches to ordering updates. The simple version is to
associate a timestamp with each update blob, and order all updates by time.
This has the advantage that it is simple, and the timestamp is a natural way
to associate time with all values by default. This is the approach taken by
Datomic and Camlistore.

Another approach is to treat updates more like commits in git. Updates would
include a reference to 'past' update blobs which modified the object(s) being
updated. This gives updates version-control semantics and lets branches and
merges happen, though it's unclear how desirable this is in practice. The other
advantage this scheme provides is strict causal ordering of updates. Since each
update includes 'past' pointers, the hash of that update securely names an
immutable history for the object.

One downside of this second approach is that it is unclear how to resolve the
state of the object when there are multiple history 'tips'. In git, this is
solved by having mutable symbolic pointers, e.g. `HEAD`. Unfortunately this
approach is less viable with an immutable storage system! The branch pointers
could be maintained locally, but that's probably too complex to be worth it.
