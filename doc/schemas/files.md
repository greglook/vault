Filesystem Data
===============

Representing a filesystem hierarchy is a common use-case, especially for
storing snapshots for archival or back-up purposes. Metadata is stored as data
blobs, which in turn reference content blobs.

## Common Metadata

Most filesystem types have a common set of metadata which defines the name,
ownership, permissions, and timestamps of the value. These are represented as
the following keys in the data blob:

```clojure
#vault/data
{:name "foo.clj"
 :posix/permissions "0755" ; octal string
 :posix/user "greglook"
 :posix/user-id 1000
 :posix/group "users"
 :posix/group-id 500
 :posix/change-time #inst "2013-10-23T20:06:13.000-00:00"
 :posix/modify-time #inst "2013-10-25T09:13:24.000-00:00"}
```

EDN doesn't support octal numbers, so the permissions are represented as a
string for readability.

## Directories

Generally, a filesystem snapshot will point to a root directory, which will in
turn reference child files and directories, forming an immutable tree of values.
Directory blobs reference their children by using links keyed by filename.

```clojure
#vault/data
{; common metadata...
 :vault/links [...]}
```

## Files

File blobs are similar to directories, only instead of linking to child entries,
they may embed or link to their file contents. See the [byte-sequence
structure](byte-sequences.md) document for more details.

```clojure
#vault/data
{; common metadata...
 ; Should contain ONE OF:
 :content #bytes/bin "iQIcBAABAgAGQ..."        ; embedded binary content
 :content #bytes/raw #vault/blob "algo:digest" ; use blob as content source
 :content #bytes/seq #vault/blob "algo:digest" ; reference a bytes structure in another blob
 }
```

## Symlinks

Symlinks are relatively simple and give a path to their target.

```clojure
#vault/data
{; common metadata...
 :filesystem.link/target "../foo/bar"}
```
