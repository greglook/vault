(ns vault.search.graph
  "Blob graph search functions and index definitions."
  (:require
    [clj-time.core :as time]
    [vault.blob.content :as content]
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
    [{:blob id
      :size (count content)
      :type (:data/type blob)
      ; TODO: label pgp keys
      :stored-at (time/now)}]))


(defn node->stats
  "Constructs an empty blob with stat metadata from a node record."
  [node]
  (assoc
    (content/empty-blob (:id node))
    :stat/size (:size node)
    :stat/stored-at (:stored-at node)
    :data/type (:type node)))


(defn get-node
  "Looks up a node record by hash-id."
  [graph id]
  (index/lookup (:nodes graph)
                {:id id}))


(defn find-nodes
  "Find node records by type keyword and label."
  [graph & [data-type label]]
  (index/find (:node-types graph)
              {:type data-type
               :label label}))



;; ## Blob Graph Links

(def link-schema
  "Schema for blob graph link records."
  {:source HashID     ; source hash-id
   :type   Keyword    ; source blob type
   :target HashID})   ; target hash-id


(defn blob->links
  "Projects a blob into graph link records."
  [blob]
  (let [record {:source (:id blob)
                :type (:data/type blob)}]
    ; TODO: walk the blob data structure and record links
    []))


(defn links-from
  "Look up data links by source hash-id."
  [graph & [source-id]]
  (index/find (:links-from graph)
              {:source source-id}))


(defn links-to
  "Look up data sorted by target hash-id and source blob type."
  [graph & [target-id data-type]]
  (index/find (:links-reverse graph)
              {:target target-id
               :type data-type}))
