Byte Sequences
==============

Large binary data can be cumbersome in a content-addressable system if it is
stored naively. If a 4 GB file is stored as a single blob, then changing the
value of one byte somehere in the middle of the file requires re-storing a new 4
GB blob. This is obviously undesirable, and the key is to _deduplicate_ as much
of the data as possible.

## Content Chunking

Deduplication strategies revolve around splitting large binary sequences into
chunks which can be stored independently. A simple strategy is to partition the
sequence every _n_ bytes. This works well when byte values are changed in-place,
because only the chunk which was changed needs to be stored as a new blob, and a
new list of the chunks can re-use all the other previously-stored chunks.

Unfortunately, fixed partitions run into trouble when data is added or removed,
because shifting the partition boundaries changes all the following chunks'
contents, and therefore hashes. Instead, we use an algorithm which calculates a
_rolling hash_ of the sequence, emitting a partition when the hash meets certain
criteria. This results in variable-sized chunks which tend to partition at the
same locations within the sequence, regardless of additions, deletions, or
changes mid-sequence.

## Sequence Structure

A byte sequence is a data structure with type `:vault/bytes` containing
vector of `:parts`, each of which are maps specifying the binary data making up
the sequence.

```clojure
{:parts
 [{:size 10000}
  {:data #bin "iQIcBAABAgAGBQ..."
   :size 84}
  {:content #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
   :size 1024}
  {:offset 492
   :parts #vault/ref "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"
   :size 500}]
 :vault/type :vault/bytes}
```

Each part in the vector must be a map containing a `:size` key. The value of
this key specifies the number of bytes in the sequence contributed by the
part. The part size must be a positive integer.

There are four types of data sources parts can reference:
- _Empty_ parts are maps containing only a `:size` key and represent an
  all-zero region of bytes, such as in a sparse file.
- _Data_ parts specify a sequence of bytes directly with `:data` giving a
  binary string value. The `:size` key should match the length of the data.
- _Content_ parts reference a blob containing the byte contents. The blob's
  hash-id is given with the `:content` key.
- _Recursive_ parts reference a blob containing another byte sequence with
  the `:parts` key.

It is an error for a part to contain more than one of `:data`, `:content`,
or `:parts`.

Content and recursive parts may specify an `:offset` key giving the number of
bytes to skip at the start of the referenced data. The `:size` key specifies the
number of bytes to take following the offset. If the part size is smaller than
the size of the source data, extra bytes are omitted. The part size (plus
offset) SHOULD NOT be larger than the source, but if so the part is padded with
trailing zero bytes.

The size of a byte sequence can be trivially calculated by summing the sizes
of the parts.

## Interface

The application layer should provide functionality to treat a reference to a
byte sequence as a normal input stream. Seeking should be supported.

```clojure
(with-open [stream (vault.app.bytes/open blob-store byte-seq)]
  (slurp stream))
```

This should produce a stream backed by a lazy seq which reads blobs from the
given blob store as needed to resolve the next bytes in the sequence.
