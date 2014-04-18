Signatures
==========

The primary usage of cryptography in Vault is to verify the ownership and
authenticity of the data in the system. Vault makes use of the OpenPGP standard
for public keys and digital signatures.

Users of a Vault store are identified by _public key_ - the key itself is stored
directly in the system as an ASCII 'armored' blob. The address of the key blob
serves as an unambiguous identifier of the entity controlling the corresponding
private key.

## Content Signing

Users can _sign_ data in the system with 'signature' structures. Signatures are
maps which may contain the following keys:
- `:data` - a reference to the data being signed (for detached signatures)
- `:key` - a reference to the PGP public key blob
- `:signature` - a byte string giving the encoded PGP signature

The `:key` and `:signature` values are required. If a signature is given as the
primary value of a blob, it is a _detached signature_ and must specify a blob
containing the contents being signed with the `:data` key. Otherwise, it is
considered an _inline signature_ and the data to be signed is the blob's primary
EDN value. These are the bytes immediately following the header through the last
byte of the last character forming the first complete EDN value.

Accordingly, an EDN value is considered to be signed by a given PGP key when:
- the value is followed by a valid inline signature with the given key, OR
- there exists a valid detached signature with the given key specifying the blob
  containing the EDN value

## Example

Here's an example of a detached signature referencing the blob "foobarbaz":

```clojure
{:data #vault/ref "sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d"
 :key #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :signature #pgp/signature #bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMrV61su7iGPQsdXvZqBb2LKUhGku6ZQxqBYOvDdXaTmYIZJBY0CtAOlTe3NXn0kvnTuaPoA6fe6Ji1mndYUudKPpWWld9vzxIYpqnxL/ZtjgjWqkDf02q7M8ogSZ7dp09D1+P5mNnS4UOBTgpQuBNPWzoQ84QP/N0TaDMYYCyMuZaSsjZsSjZ0CcCm3GMIfTCkrkaBXOIMsHk4eddb3V7cswMGUjLY72k/NKhRQzmt5N/4jw/kI5gl1sN9+RSdp9caYkAumc1see44fJ1m+nOPfF8G79bpCQTKklnMhgdTOMJsCLZPdOuLxyxDJ2yte1lHKN/nlAOZiHFX4WXr0eYXV7NqjH4adA5LN0tkC5yMg86IRIY9B3QpkDPr5oQhlzfQZ+iAHX1MyfmhQCp8kmWiVsX8x/mZBLS0kHq6dJs//C1DoWEmvwyP7iIEPwEYFwMNQinOedu6ys0hQE0AN68WH9RgTfubKqRxeDi4+peNmg2jX/ws39C5YyaeJW7tO+1TslKhgoQFa61Ke9lMkcakHZeldZMaKu4Vg19OLAMFSiVBvmijZKuANJgmddpw0qr+hwAhVJBflB/txq8DylHvJJdyoezHTpRnPzkCSbNyalOxEtFZ8k6KX3i+JTYgpc2FLrn1Fa0zLGac7dIb88MMV8+Wt4H2d1c"
 :vault.data/type :pgp/signature}
```
