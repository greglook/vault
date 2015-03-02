Identity and State
==================

Immutable data is great, but to be useful the system needs to be able to
represent _mutable state_ somehow. Say a user stores a document; that content
has some address based on its hash digest. If the user updates the document,
they must keep a different address for the new version. The concept tying the
different versions of the document together is _identity_.

For motivation, suppose a user wants to store a document in Vault. The first
version will have a certain hash identifier. When the user wants to make updates
however, the new version of the document will have a new identifier! Users have
to juggle remembering the last identifier for their data.

## Mutable Namespaces

To address these problems, a [public key blob](keys.md) establishes a
_namespace_ which assigns a stable identity to a sequence of blob values. The
hash of the public key provides a stable identifier to reference the changing
values by. The 'current root' of the namespace is _bound_ to new values by
adding _transaction blobs_:

```clojure
#vault/data
{:vault/type :vault/tx
 :time #inst "2013-10-25T09:13:24.000-00:00",
 :ancestors #{#vault/blob "sha256:00f916cce73e53b5618069817fb451853e0b9f7529fe8e1c6f34615666322037"
              #vault/blob "sha256:1d5f5e2dcf673e40556b26f4617598cfa26a46f192a18d2f808f6be6b36756cb"},
 :bindings {#vault/blob "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
            #vault/blob "sha256:e0fb5ab696858a047fd0c8e7f42a98b1649306a159084c9126b6c38a13f6459c",
            #vault/blob "sha256:dac39f5db1bbbbf88b9a35f9df57eaa4e6248e56a014f54bc2b0792c636aaa5b"
            #vault/blob "sha256:5667cf1cb367388af4f5827d89d8776d5a6210a7e64a785cd85a20bf8c6d8dcb"}}

#vault/signature
{:key #vault/blob "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75",
 :signature #pgp/signature #bytes/bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMr..."}

#vault/signature
{:key #vault/blob "sha256:dac39f5db1bbbbf88b9a35f9df57eaa4e6248e56a014f54bc2b0792c636aaa5b",
 :signature #pgp/signature #bytes/bin "AgAGBQJSeAoJEAadbp3eAHKNATs56iQIcBA/2W5QsCPHABckP5SMr..."}
```

Note that the transaction can bind multiple namespaces at once in an _atomic_
way. This requires that the blob contain signatures from all affected
namespaces.

## Causal Ordering

All transaction blobs are timestamped, so modifications to a namespaces can be
ordered in time. However, transaction blobs _also_ contain an `:ancestors`
field, giving them version-control semantics. In this way, a _namespace_ in
Vault is comparable to a _branch_ in a version-control system like Git.

The hash-based reference to past transactions enforces a strict causal ordering
of updates. Transactions build up a merkle hash chain which provides a secure
history of the mutations to a namespace.
