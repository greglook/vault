Identity and State
==================

Immutable data is great, but to be useful the system needs to be able to
represent _mutable state_ somehow. Say a user stores a document; that content
has some address based on its hash digest. If the user updates the document,
they must keep a different address for the new version. The concept tying the
different versions of the document together is _identity_.

## Motivation

As an example, suppose some data representing a financial transaction includes
`to` and `from` attributes. The simplest way to identify the accounts involved
would be to use a string to name them. Unfortunately, this does not scale well
if more information about the account needs to be stored.

An alternative is to refer to a blob of metadata about the account, but this is
also brittle! Because data is immutable, if we need to update the metadata about
the account, all the data must be re-stored with an updated hash reference.

## Transaction Blobs

To get around these problems, _entities_ assign a continuous identity to a
collection of _attributes_ as they change over time. Entities are defined by
data in a _root_ blob. The address of this root provides a stable identifier to
reference the entity by.

```clojure
{:vault/type :vault.entity/root
 :source-id "a123d1c0f82f2ea1"
 :owner #vault/ref "sha256:00f916cce73e53b5618069817fb451853e0b9f7529fe8e1c6f34615666322037"
 :time #inst "2013-10-23T20:06:13Z"
 :data
 [[:attr/set :name "Example Entity"]
  [:attr/set :content #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"]
  [:attr/add :tags "foo"]
  [:attr/add :tags "bar"]]}
```

Changes to the entity are enacted by adding further _update blobs_ to the store.
These blobs specify modifications to the entity, so the 'current' state of the
entity can be determined by applying the full sequence of updates made to it.
This history allows the state of the entity at any time can be recovered by
selecting which updates to apply.

```clojure
{:vault/type :vault.entity/update
 :time #inst "2013-10-25T09:13:24.000-00:00"
 :data
 {#vault/ref "sha256:00f916cce73e53b5618069817fb451853e0b9f7529fe8e1c6f34615666322037"
  [[:attr/set :name "A Thing"]
   [:attr/add :tags "foo"]
   [:attr/add :tags "bar"]
   [:attr/del :tags "bar"]]
  #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
  [[:attr/del :description]
   [:attr/set :content #vault/ref "sha256:53e0b9f7503729f698174615666322f00f916cceb4518e8e1c6f373e53b56180"]]}}
```

Root and update blobs are _transactional_, meaning they apply atomically to the
states of the relevant entities.

Entity roots and updates must be signed to be considered valid.

## Attributes

Entities act like a map, grouping together named _attributes_. These specify
relationships between entities and values. Attribute values must be primitive
types, such as booleans, integers, doubles, strings, or hash-ids. Attributes can
be single-valued (such as `:title`), or multi-valued (such as `:tags`).
Multi-valued attributes are represented as unordered sets.

Attributes are named by keywords, which should generally be namespaced. Some
potential system-level attributes are:
- `:ident` - a (unique) symbolic shortcut to give to the entity
- `:title` - a string naming the entity
- `:description` - a longer string describing the entity in detail
- `:tags` - set of string tags labeling the entity

## Datoms

A _datom_ is an atomic fact about an entity. Datoms apply some _operation_ to a
single attribute of an entity. Entity roots and updates may contain a sequence
of datoms about entities.

Valid operations are:
- `:attr/set` - Sets the value of an attribute to a single, provided value.
- `:attr/add` - Adds a value to a multi-valued attribute. If the attribute was
  single-valued prior to the operation, the result is a set containing both it
  and the new value.
- `:attr/del` - Removes an attribute value. If the value given to the operation
  is nil, the attribute is completely removed from the entity. If the attribute
  is multi-valued, the operation's value is removed from the set if it is
  present. If the attribute is single-valued, the attribute is removed if the
  value matches the operation's value.

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
