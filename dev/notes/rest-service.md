Rest Server
===========

## Blob Service

```
   PUT /blobs/{hash-id}
   GET /blobs/{hash-id}
   GET /blobs{?prefix,after,limit}
```

## Data Service

```
  POST /data/                         ; create a data structure blob
   GET /data/{hash-id}                ; get a blob as a data structure
```

## State Service

```
  POST /state/                       ; create a new entity *
   PUT /state/{entity-id}            ; update an entity's attributes *
   GET /state/{entity-id}            ; get the current state of an entity
   GET /state/{entity-id}/history    ; list updates affecting the entity
DELETE /state/{hash-id}              ; mark an entity root or update blob as deleted *
   GET /state{?attrs}
```

## Index Service

```
  POST /index/                                ; record a blob in the index
   GET /index/type/{type}                     ; index on :vault/type
   GET /index/eavt/{entity}/{attr}/{value}    ; entity attributes
   GET /index/aevt/{attr}/{entity}/{value}    ; entities with attribute
   GET /index/avet/{attr}/{value}/{entity}    ; indexed attributes
   GET /index/vaet/{value}/{attr}/{entity}    ; reverse index
```

### Type Index

Indexes blobs by 'type'. Supported by the following table:

```
VARCHAR     hash_id         PRIMARY KEY
INTEGER     size            NOT NULL
VARCHAR     type
TIMESTAMP   stored_at       NOT NULL
```

Indexed on:
- `[hash_id]` for blob enumeration (also, the public key)
- `[type stored_at]` for type lookup
- `[stored_at]` potentially for store history
