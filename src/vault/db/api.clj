(ns vault.db.api
  "API for writing applications on top of Vault.")


;; ## No equivalents

;; - `(add-listener fut f executor)`
;; - `(connect uri)`
;; - `(create-database uri)`
;; - `(db connection)`
;; - `(delete-database uri)`
;; - `(entid db ident)`
;; - `(entid-at db part t-or-date)`
;; - `(filter db pred)`
;; - `(ident db eid)`
;; - `(invoke db eid-or-ident & args)`
;; - `(is-filtered db)`
;; - `(log connection)`
;; - `(next-t db)`
;; - `(part eid)`
;; - `(release conn)`
;; - `(remove-tx-report-queue connection)`
;; - `(rename-database uri new-name)`
;; - `(resolve-tempid db tempids tempid)`
;; - `(shutdown shutdown-clojure)`


(defn gc-storage
  "Allow storage to reclaim garbage older than a certain age."
  [connection older-than]
  nil)


(defn request-index
  "Schedules a re-index of the database. The re-indexing happens
  asynchronously. Returns true if re-index is scheduled."
  [connection]
  nil)


;; ## Database Views

(defn basis-t
  "Returns the t of the most recent transaction reachable via this db value."
  [db]
  nil)


(defn as-of
  "Returns the value of the database as of some point t, inclusive. `t` can be
  a transaction hash-id or `DateTime`."
  [db t]
  nil)


(defn as-of-t
  "Returns the as-of point, or nil if none."
  [db]
  nil)


(defn since
  "Returns the value of the database since some point t, exclusive
  t can be a transaction number, transaction ID, or Date."
  [db t]
  nil)


(defn since-t
  "Returns the since point, or nil if none."
  [db]
  nil)


(defn history
  "Returns a special database containing all assertions and retractions across
  time. This special database can be used for datoms and index-range calls and
  queries, but not for entity or with calls. as-of and since bounds are also
  supported. Note that queries will get all of the additions and retractions,
  which can be distinguished by the fifth datom field :added (true for
  add/assert) [e a v tx added]"
  [db]
  nil)



;; ## Transaction Log

(defn log
  "Retrieves a value of the log for reading."
  [connection]
  nil)


(defn tx-range
  "Returns a range of transactions in log, starting at start,
  or from beginning if start is nil, and ending before end, or through
  end of log if end is nil. start and end can be can be a transaction
  number, transaction ID, Date or nil."
  [log start end]
  nil)



;; ## Schema Functions

(defn attribute
  [db attr-key]
  nil)



;; ## Data Access

(defn datoms
  "Raw access to the index data, by index. The index must be supplied, and,
  optionally, one or more leading components of the index can be supplied to
  narrow the result."
  [db index & components]
  nil)


(defn seek-datoms
  "Raw access to the index data, by index. The index must be supplied, and,
  optionally, one or more leading components of the index can be supplied for
  the initial search.  Note that, unlike the datoms function, there need not be
  an exact match on the supplied components.  The iteration will begin at or
  after the point in the index where the components would reside.  Further, the
  iteration is not bound by the supplied components, and will only terminate at
  the end of the index. Thus you will have to supply your own termination
  logic, as you rarely want the entire index. As such, seek-datoms is for more
  advanced applications, and datoms should be preferred wherever it is
  adequate."
  [db index & components]
  nil)


(defn index-range
  "Returns an Iterable range of datoms in an index, starting at start, or from
  beginning if start is nil, and ending before end, or through the end of index
  if end is nil."
  [db index start end]
  nil)


(defn entity
  "Returns a dynamic map of the entity's attributes for the given hash-id.
  Entities implement

  - `clojure.lang.Associative`
  - `clojure.lang.ILookup`
  - `clojure.lang.IPersistentCollection`
  - `clojure.lang.Seqable`
  - `datomic.Entity`"
  [db eid]
  nil)


(defn entity-db
  "Returns the database value that is the basis for this entity."
  [entity]
  nil)


(defn q
  "Executes a query against inputs."
  [query & inputs]
  nil)



;; ## Data Writing

(defn transact
  "Submits a transaction to the database for writing."
  [connection tx-data]
  nil)


(defn with
  "Applies tx-data to the database. It is as if the data was
  applied in a transaction, but the source of the database is
  unaffected. Takes data in the same format expected by transact, and
  returns a map similar to the map returned by transact."
  [db tx-data]
  nil)
