Key Blobs
=========

Vault makes use of the OpenPGP standard for public keys and digital signatures.
Users of Vault are identified by _public key_ - the key itself is stored
directly in the system as an ASCII 'armored' blob. The address of the key blob
serves as an unambiguous identifier of the person controlling the corresponding
private key.

```
-----BEGIN PGP PUBLIC KEY BLOCK-----
Version: BCPG v1.50

mI0EUr3KFwEEANAfzcKxWqBYhkUGo4xi6d2zZy2RAewFRKVp/BA2bEHLAquDnpn7
abgrpsCFbBW/LEwiMX6cfYLMxvGzbg5oTfQHMs27OsnKCqFas9UkT6DYS1PM9u4C
3qlJytK9AFQnSYOrSs8pe6VRdeHZb7FM+PawqH0cuoYfcMZiGAylddXhABEBAAG0
IVRlc3QgVXNlciA8dGVzdEB2YXVsdC5tdnhjdmkuY29tPoi4BBMBAgAiBQJSvcoX
AhsDBgsJCAcDAgYVCAIJCgsEFgIDAQIeAQIXgAAKCRCSOxwcQ5IxioerBACBfXLk
j4ryCBcmJ+jtL1/W5jUQ/E4LWW7gG34tkHBJk00YulXUe4lwY1x9//6zowVV7DvW
ndmjcb0R6Duw76+xLsnEKomW0aV+ydoTtFZ7bX0kChX/IegPLMB0OnenzOoWKieN
GLFUpzICH+i7Or5X2bmekHtnbHPfJmQAuvmUnrACAAA=
=3aKz
-----END PGP PUBLIC KEY BLOCK-----
```

## Data Signatures

Users can sign data in the system with signature structures. Signatures are maps
which follow the primary value in a data blob, and reference the public key blob
of the owner.

```clojure
#vault/signature
{:key #vault/ref "sha256:461566632203729fe8e1c6f373e53b5618069817f00f916cceb451853e0b9f75"
 :signature #pgp/signature #bytes/bin "iQIcBAABAgAGBQJSeHKNAAoJEAadbp3eATs56ckP/2W5QsCPH5SMr..."}
```

The signature must match the UTF-8 encoded byte sequence of the characters which
form the primary value in the blob (not including the header).
