(ns vault.index.engine)


;;;;; ENGINE PROTOCOL ;;;;;

(defprotocol SearchEngine
  "Protocol for an engine which implements index searches."

  (init!
    [this]
    "Initializes any state necessary for the engine to function. For example,
    creating a database table from the index record schema.")

  (update!
    [this record]
    "Adds a record to the index for searching.")

  (search*
    [this query opts]
    "Search the index for records with values matching the given query."))


(defn search
  "Search the index for records with values matching the given query.

  Options may include:
  * :ascending   Wether to return the sequence in ascending order."
  ([engine query]
   (search* engine query nil))
  ([engine query opts]
   (search* engine query opts))
  ([engine query opt-key opt-val & opts]
   (search* engine query (apply hash-map opt-key opt-val opts))))



;;;;; UTILITY FUNCTIONS ;;;;;

(defn matches?
  "Determines whether every key set in the pattern map has an equal value in
  the given record."
  [pattern record]
  (every? #(= (get pattern %)
              (get record %))
          (keys pattern)))
