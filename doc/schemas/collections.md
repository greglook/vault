Organizing Data
===============

Another desired feature of the system is an organizational layer on top of the
permanode entities. In effect, one or more heirarchies of collections. In
practice, each collection will be a permanode representing a dynamic set. Each
collection contains references to permanodes and potentially other
sub-collections.

Ideally, this could be exposed as a FUSE filesystem which gives the top-level
'roots', i.e. collections which are not contained in any other collections.

POTENTIAL PROBLEM: how to deal with circular containment? Taxonomies have to be
trees. Can check at runtime, but nothing to stop the data from showing up.
Maybe report the error and use timestamps to 'resolve' the conflict by picking
the first (or last) one added to win?

For example, say we have:
* Collection A: [B, C, X]
* Collection X: [Y, Z, A]
If no other collections contain either A or X, then these two collections will
not be accessible from a list of 'roots' as described above.
