(ns vault.data
  "Code to handle structured data, usually represented as EDN."
  (:require [clojure.string :as string]
            [clojure.data.codec.base64 :as b64]))


;; TOTAL-ORDERING COMPARATOR

(defn total-order
  "Comparator function that provides a total-ordering of EDN values."
  [a b]
  (compare (pr-str a) (pr-str b))) ; FIXME: dirty hack



;; EDN-TAGGED VALUE PROTOCOL

(defprotocol TaggedValue
  (tag [this] "Return the tag symbol to apply to the value.")
  (value [this] "Return the value to pass to the tag."))


(extend-protocol TaggedValue
  (Class/forName "[B")
  (tag [this] 'bin)
  (value [this] (apply str (map char (b64/encode this))))

  java.util.Date
  (tag [this] 'inst)
  (value [this]
    (let [utc-format (doto (java.text.SimpleDateFormat.
                             "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00")
                       (.setTimeZone (java.util.TimeZone/getTimeZone "GMT")))]
      (.format utc-format this)))

  java.util.UUID
  (tag [this] 'uuid)
  (value [this] (str this)))



; TODO: add support for reading #bin strings
