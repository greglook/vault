(ns vault.entity.datom
  "Functions for handling 'datoms', atomic data assertions. Datoms consist of
  six attributes:

  - `op` the operation being performed
  - `entity` the hash-id of the root blob
  - `attribute` keyword naming the attribute being operated on
  - `value` the attribute value
  - `tx` hash-id of the transaction blob containing the datom
  - `time` instant when the datom was asserted")


(defrecord Datom
  [op entity attribute value tx time])


(defn apply-datom
  "Applies a datom to an entity state map to produce an updated state value.
  The operation must be one of:

  - `:attr/set` to set a single-valued attribute to the given value
  - `:attr/add` to add a value to a multi-valued attribute
  - `:attr/del` to remove a value from an attribute"
  [entity {:keys [op attribute value]}]
  (let [current (get entity attribute)]
    (case op
      :attr/set
      (assoc entity attribute value)

      :attr/add
      (assoc entity attribute
        (cond
          (set? current) (conj current value)
          (nil? current) (sorted-set value)
          :else          (sorted-set current value)))

      :attr/del
      (cond
        (nil? value)
        (dissoc entity attribute)

        (set? current)
        (let [new-set (disj current value)]
          (if (empty? new-set)
            (dissoc entity attribute)
            (assoc entity attribute new-set)))

        (= current value)
        (dissoc entity attribute)

        :else
        entity))))


(defn entity-state
  "Given a sequence of datoms, return a map giving the 'current' state of some
  entity."
  [root-id datoms]
  (reduce
    apply-datom
    {:vault.entity/id root-id}
    datoms))
