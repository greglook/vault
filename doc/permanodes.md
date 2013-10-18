# Permanodes

_Permanodes_ let you assign a continuous identity to a sequence of values, much
like Clojure's reference types (namely, atoms). Thus, permanodes represent
_entities_ and track the current state of that entity over time. Obviously, the
permanode blobref is static, so changes are accomplished by the addition of
_claim_ blobs to the data store. A claim specifies a _modification_ to an
existing permanode, and thus the 'current' state of the entity can be determined
by applying the full sequence of changes made to it.

Claims should include a reference to the 'prior' permanode state. This gives
version-control semantics and lets branches and merges happen.

TODO: find better terminology

## "Mutable" State

TODO: discuss how modifying permanodes works

## Metadata

Permanodes can store metadata about the values they represent. Some simple ideas
are `title`, `description`, etc. Can also include type/schema information, like
registering custom unit equivalents for physical quantities in another blob and
referring to it from the permanode attributes.

## Example

When data includes a reference to an _identity_, the attribute should refer to a
permanode representing that entity. As an example, a financial transaction may
include `to` and `from` attributes. The simplest case would be to use a string
designating the entities, but this does not scale well if more information about
the entity should be stored. An alternative is to refer to a blob of metadata
about the entity, but this is brittle - if the metadata changes, all the
transactions must be re-stored with an updated hash reference. The better design
is to store transactions which refer to the permanode representing the entities
in question, which can then be updated at will. If necessary, the state of the
entity at an arbitrary time can be recovered by replaying the changes up to
then.
