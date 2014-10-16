TODO
----

- record type for an entity which implements IAssociative and friends, and can
  look up references from the blob-store.

## Tool
Maybe switch to using profiles? E.g. keep my 'main' data store separate from
media or 'sensitive' data. In general support more public/private delineation?

Tool commands:
```
help [command] [subcommand...]

config dump

blob list [opts] [prefix]
blob stat <hash-id> [hash-id...]
blob get <hash-id>
blob put [file]

data show <hash-id> [hash-id...]

entity create ...
entity update <hash-id> ...
```

## Integrity Checks
Data integrity checkers:
- blobs' content matches their stored hashes
- entity roots and updates are syntactically valid
- signatures on entities and updates are valid
- check for 'garbage collectable' blobs

## Signing
- find a Java library for Unix Domain Socket communication?
- use gpg-agent/gnome-keyring/osx keychain to sign a hash via PKCS#11

## Indexing
- brute-force 'index'
- in-memory index

## Application Schemas
Start working on application-layer data formats for storage in Vault.
Some good initial targets are probably:
- emails, because I have lots of them and the data model is fairly simple.
- filesystem snapshots (files/dirs/links/etc)

An implementation of the rolling checksum algorithm to chunk up large binary
data would be good too.
