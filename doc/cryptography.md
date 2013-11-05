# Cryptography

The primary usage of cryptography in Vault is to verify the ownership and
authenticity of the data in the system. Vault makes use of the OpenPGP standard
for public keys and digital signatures.

Users of a Vault store are identified by _public key_ - the key itself is stored
directly in the system as an ASCII 'armored' blob. The address of the key blob
serves as an unambiguous identifier of the entity controlling the corresponding
private key.

## Content Signatures

Users can _sign_ data in the system with 'signature' structures. Signatures are
maps which may contain the following keys:
- `:key` - a reference to the PGP public key blob
- `:signature` - a byte string giving the encoded PGP signature
- `:target` - signature target (see below)

The key and signature values are required. The _target_ of a signature may
either be a blob reference or an integer index value. If the target refers to a
blob, the entire blob contents are the bytes being signed. If an integer value
is given, it is treated as an index into the collection of EDN values in the
blob containing the signature structure. The implicit target is `0`, meaning the
first EDN value in the blob. This simplifies the common case where a value and
its signature are given in the same blob.

In the index case, the bytes to be signed are those that form the full value
representation **AS PRINTED IN THE BLOB** excluding any leading or trailing
whitespace. For example, a tagged map should include the bytes forming the '#'
character though those forming the closing '}'.

## Example

Here's an example of a detached signature referencing the blob "foobarbaz":

```clojure
#vault/signature
{:key #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :signature #bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMrV61su7iGPQsdXvZqBb2LKUhGku6ZQxqBYOvDdXaTmYIZJBY0CtAOlTe3NXn0kvnTuaPoA6fe6Ji1mndYUudKPpWWld9vzxIYpqnxL/ZtjgjWqkDf02q7M8ogSZ7dp09D1+P5mNnS4UOBTgpQuBNPWzoQ84QP/N0TaDMYYCyMuZaSsjZsSjZ0CcCm3GMIfTCkrkaBXOIMsHk4eddb3V7cswMGUjLY72k/NKhRQzmt5N/4jw/kI5gl1sN9+RSdp9caYkAumc1see44fJ1m+nOPfF8G79bpCQTKklnMhgdTOMJsCLZPdOuLxyxDJ2yte1lHKN/nlAOZiHFX4WXr0eYXV7NqjH4adA5LN0tkC5yMg86IRIY9B3QpkDPr5oQhlzfQZ+iAHX1MyfmhQCp8kmWiVsX8x/mZBLS0kHq6dJs//C1DoWEmvwyP7iIEPwEYFwMNQinOedu6ys0hQE0AN68WH9RgTfubKqRxeDi4+peNmg2jX/ws39C5YyaeJW7tO+1TslKhgoQFa61Ke9lMkcakHZeldZMaKu4Vg19OLAMFSiVBvmijZKuANJgmddpw0qr+hwAhVJBflB/txq8DylHvJJdyoezHTpRnPzkCSbNyalOxEtFZ8k6KX3i+JTYgpc2FLrn1Fa0zLGac7dIb88MMV8+Wt4H2d1c"
 :target #vault/ref "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"}
```
