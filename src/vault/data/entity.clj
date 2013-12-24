(ns vault.data.entity
  "Entity handling functions.")



(defrecord EntityRoot [owner id time attributes])


(defrecord EntityUpdate [time updates])


(defrecord EntityDeletion [time target])


;(data/extend-tagged-map ObjectRoot   vault/object.root)
;(data/extend-tagged-map ObjectUpdate vault/object.update)


; TODO: 'reducing' function which takes an object root and a sequence of
; updates and returns the 'current' state of the object.
