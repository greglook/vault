(ns vault.search.index)


(defprotocol Index
  "Protocol for a single index."

  (search
    [this pattern opts]
    "Search the index for records with values matching the given pattern.
    Options may include:
    - :ascending   Whether to return the sequence in ascending order.")

  (update!
    [this record]
    "Adds a record to the index for searching."))
