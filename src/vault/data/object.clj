(ns vault.data.object
  "Object handling functions."
  (:require
    (vault
      [blob :as blob]
      [data :as data])))



(defrecord ObjectRoot [id time owner content attributes])


(defrecord ObjectUpdate [time past updates])


(data/extend-tagged-map ObjectRoot   vault/object.root)
(data/extend-tagged-map ObjectUpdate vault/object.update)


; TODO: 'reducing' function which takes an object root and a sequence of
; updates and returns the 'current' state of the object.
