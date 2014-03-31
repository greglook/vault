Rest Server
===========

## Blob Service

   PUT /blobs/{hash-id}
   GET /blobs/{hash-id}
   GET /blobs{?prefix,after,limit}

## Data Service

  POST /data/                         ; create a data structure blob
   GET /data/{hash-id}                ; get a data structure blob

## State Service

  POST /entities/                     ; create a new entity *
   PUT /entities/{root-id}            ; update an entity's attributes *
   GET /entities/{root-id}            ; get the current state of an entity
   GET /entities/{root-id}/history    ; list updates affecting the entity
DELETE /entities/{root-id}            ; mark an entity root or update blob as deleted *
   GET /entities{?attrs}

## Index Service

  POST /index/
   GET /index/type/{ns}/{name}                ; index on :vault/type
   GET /index/eavt/{entity}/{attr}/{value}
   GET /index/aevt/{attr}/{entity}/{value}
   GET /index/avet/{attr}/{value}/{entity}    ; indexed attributes
   GET /index/vaet/{ref}/{attr}/{entity}      ; reverse index
