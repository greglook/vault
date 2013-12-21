# Identity and State

Immutable data is great, but to be useful the system needs to be able to
represent _mutable state_ somehow. Say a user stores a document; that content
has some address based on its hash digest. If the user updates the document,
they must keep a different address for the new version. The concept tying the
different versions of the document together is _identity_.

As an example, suppose some data representing a financial transaction includes
`to` and `from` attributes. The simplest way to identify the accounts involved
would be to use a string to name them. Unfortunately, this does not scale well
if more information about the account needs to be stored.

An alternative is to refer to a blob of metadata about the account, but this is
also brittle! Because data is immutable, if we need to update the metadata about
the account, all the data must be re-stored with an updated hash reference.

To get around these problems, _entities_ assign a continuous identity to a
collection of attributes as they change over time. Entities are defined by data
in a _root_ blob. This provides a stable address to reference the entity by.

Changes to the entity are enacted by adding further _update blobs_ to the store.
These blobs specify modifications to the entity, so the 'current' state of the
entity can be determined by applying the full sequence of updates made to it.
Actually, the state of the entity at _any_ time can be recovered by selecting
which updates to apply.

## Entity Attributes

Entities act like a map, grouping together named _attributes_. These specify
relationships between entities and values. Attribute should be given relatively
primitive values, preferring to reference more complex data indirectly. Values
may be either a primitive, such as a boolean, integer, double, or string, or a
blobref. Attributes may either be scalars (single values) or sets (multiple
values).

Attributes are named by keywords, which should generally be namespaced.
- `:identity` - a (unique) symbolic shortcut to give to the entity
- `:content` - the 'state value' of the entity
- `:title` - string naming the entity
- `:description` - longer string describing the entity
- `:tags` - set of string tags labeling the entity
- `:http/content-type` - MIME type for the entity's content

## Temporal Ordering

All update blobs are timestamped, so all modifications to an entity can be
ordered in time. Updates also have a second timestamp, built into the signature
on the blob - this can be considered the 'creation date' of the fact, whereas
the 'main' timestamp should be the true historical moment the update was
effective. Generally, these two will have the same value.

Another approach to ordering is to treat updates more like commits in git.
Updates would include a reference to 'past' update blobs which modified the
entity(s) being updated. This gives updates version-control semantics and lets
branches and merges happen. The other advantage this scheme provides is strict
causal ordering of updates. Since each update includes 'past' pointers, the hash
of that update securely names an immutable history for the entity.

One downside of this second approach is that it is unclear how to resolve the
state of the entity when there are multiple history 'tips'. In git, this is
solved by having mutable symbolic pointers, e.g. `HEAD`. Unfortunately this
approach is less viable with an immutable data system! Another downside is the
case of wanting to delete a past blob. Deletions would be tricky without
invalidating causal links.
