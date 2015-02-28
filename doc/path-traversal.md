Data Link Paths
===============

Structured data in Vault can be _linked_ to other data by providing a vector of
_path keys_ and their corresponding hash identifier links in the `:vault/links`
attribute. This provides a generic way to address tree-like data structures.

```clojure
#vault/data
{:vault/links [{:name "bar", :target #vault/blob "sha256:..."}
               {:name "baz", :target #vault/blob "sha256:..."}
               ...]}
```

If blob A links to blob B with the "foo" key, then the uri
`sha256:<hash-of-A>/foo` will resolve to blob B. Similarly, if blob B links to C
as "bar", and C links to D as "baz", then the following URIs all resolve to the
same blob:

```
sha256:<hash-of-A>/foo/bar/baz
sha256:<hash-of-B>/bar/baz
sha256:<hash-of-C>/baz
sha256:<hash-of-D>
```

Each blob can only make single-level links in the path, so `:name` must not
contain the path separator (`/`). Links in the `:vault/links` attribute must be
sorted descending by name.

## B-tree Scaling

The reality is a little more complicated for larger sets of links, which can
occur when adding many children to a single parent. The naive approach would
require rewriting the entire set of links (and any other content in the blob),
making access time grow with O(n) and storage space with O(n^2), which is
obviously not scalable.

Instead, above a certain _branching factor_, the vector of links begins acting
like a [B-tree](http://en.wikipedia.org/wiki/B-tree), including child trees
separated by the traditional link entries. When resolving a path segment, the
search will recurse into the child trees as necessary. The subtrees should link
to data blobs of a similar vector of parts:

```clojure
#vault/data
[{:tree #vault/blob "sha256:...", :count 14}
 {:name "bar.dat", :target #vault/blob "sha256:..."}
 {:tree #vault/blob "sha256:...", :count 20}
 {:name "foo", :target #vault/blob "sha256:..."}
 {:tree #vault/blob "sha256:...", :count 8}]}
```

Note that each subtree contains the number of path elements found under it,
enabling both total-size and find-by-offset queries.
