(ns vault.search.graph
  "Blob graph search functions and index definitions."
  (:require
    [clj-time.core :as time]
    [vault.blob.content :as content]
    [vault.blob.store :as store]
    [vault.search.index :as index])
  (:import
    clojure.lang.Keyword
    org.joda.time.DateTime
    vault.blob.content.HashID))


;; ## Blob Graph Nodes

(def node-schema
  "Schema for blob graph node records which store blob stats."
  {:id        HashID      ; blob hash-id (pk)
   :size      Long        ; blob byte length
   :type      Keyword     ; data type
   :label     String      ; type-specific annotation
   :stored-at DateTime})  ; time added to index


(defn blob->node
  "Projects a blob into a node record."
  [blob]
  (when-let [{:keys [id content]} blob]
    {:blob id
     :size (count content)
     :type (:data/type blob)
     ; TODO: label pgp keys
     :stored-at (time/now)}))


(defn node->stats
  "Constructs an empty blob with stat metadata from a node record."
  [node]
  (assoc
    (content/empty-blob (:id node))
    :stat/size (:size node)
    :stat/stored-at (:stored-at node)
    :data/type (:type node)))



;; ## Blob Graph Links

(def link-schema
  "Schema for blob graph link records."
  {:source HashID     ; source hash-id
   :type   Keyword    ; source blob type
   :key    Keyword    ; if the blob is a map, top-level key the link was found under
   :target HashID})   ; target hash-id


(defn blob->links
  "Projects a blob into graph link records."
  [blob]
  (let [record {:source (:id blob)
                :type (:data/type blob)}]
    ; TODO: walk the blob data structure and record links
    []))



;; ## Graph Protocol

(defprotocol GraphCatalog
  "Graph catalogs store records about the nodes and the links among them in the
  blob graph."

  (get-node
    [graph id]
    "Looks up a node record by hash-id.")

  (find-nodes
    [graph data-type label]
    "Find node records by type keyword and optional label string.")

  (links-from
    [graph source-id]
    "Look up data links by source hash-id.")

  (links-to
    [graph target-id data-type]
    "Look up data sorted by target hash-id and source blob type."))


;; The brute-force graph catalog iterates over all the blobs in a store to
;; determine the answers to the graph queries.
(defrecord BruteGraphCatalog
  [store]

  GraphCatalog

  (get-node
    [this id]
    (some->> id (store/get* store) blob->node))


  (find-nodes
    [this data-type label]
    nil)


  (links-from
    [this source-id]
    nil)


  (links-to
    [this target-id data-type]
    nil))


;; An indexed graph catalog stored blob node and link records in sorted index
;; data structures, which makes lookups much more efficient.
(defrecord IndexGraphCatalog
  [nodes node-types forward-links reverse-links]

  GraphCatalog

  (get-node
    [this id]
    (index/lookup nodes {:id id}))


  (find-nodes
    [this data-type label]
    (index/find node-types
                {:type data-type
                 :label label}))


  (links-from
    [this source-id]
    (index/find forward-links
                {:source source-id}))


  (links-to
    [this target-id data-type]
    (index/find reverse-links
                {:target target-id
                 :type data-type})))
