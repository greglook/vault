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
{:name "foo.clj"
 :permissions "0755" ; octal string
 :owner-id 1000
 :owner "greglook"
 :group-id 500
 :group "users"
 :change-time #inst "2013-10-23T20:06:13.000-00:00"
 :modify-time #inst "2013-10-25T09:13:24.000-00:00"}
```

EDN doesn't support octal numbers, so the permissions are represented as a
string for readability.

## Directories

Generally, a filesystem snapshot will point to a root directory, which will in
turn reference child files and directories, forming an immutable tree of values.

Directory blobs reference a second data blob which is a static set containing
the hash-ids of the entries of the directory. This separation lets changes to
the directory metadata (such as renaming) avoid re-storing the entire list of
children references.

```clojure
{:vault/type :filesystem/directory
 ; common metadata...
 :entries #vault/ref "algo:digest"}
```

## Files

File blobs are similar to directories, only instead of referencing a static set
of child entries, they reference a blob which defines their content. This is
specified either as a direct reference to a content blob or to a
[byte-sequence](byte-sequences.md) data blob.

```clojure
{:vault/type :filesystem/file
 ; common metadata...
 ; Files contain ONE of the two following entries:
 :byte/content #vault/ref "algo:digest" ; direct reference to file contents
 :byte/parts #vault/ref "algo:digest"}  ; reference to a :byte/seq structure
```

## Symlinks

Symlinks are simple and give a path to their target.

```clojure
{:vault/type :filesystem/symlink
 ; common metadata...
 :target "../foo/bar"}
```
