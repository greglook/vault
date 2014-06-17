(ns vault.index.search)


(defprotocol Engine
  "Protocol for an engine which implements index searches."

  (search
    [this pattern opts]
    "Search the index for records with values matching the given pattern.
    Options may include:
    - :ascending   Whether to return the sequence in ascending order.")

  (update!
    [this record]
    "Adds a record to the index for searching."))


(defn matches?
  "Determines whether every key set in the pattern map has an equal value in
  the given record."
  [pattern record]
  (every? #(= (get pattern %)
              (get record %))
          (keys pattern)))
