(ns vault.print
  "Utilities for canonical printing of EDN values."
  (:require ansi
            [clojure.string :as string]
            [clojure.data.codec.base64 :as b64]
            [fipp.printer :refer [defprinter]]
            [vault.data :as data]))


;; CONTROL VARS

(def ^:dynamic *colored-output*
  "Output ANSI colored output from print functions."
  false)


(defmacro with-colored-output
  "Performs the given forms with colored output enabled."
  [& body]
  `(binding [*colored-output* true]
    ~@body))


(def ^:dynamic *strict-mode*
  "If set, throw an exception if there is no defined canonical print method for
  a given value."
  false)


(defmacro with-strict-mode
  "Performs the given forms with strict mode enabled."
  [& body]
  `(binding [*strict-mode* true]
    ~@body))



;; COLORING FUNCTIONS

(defn- color-text
  "Constructs a text doc, which may be colored if *colored-output* is true."
  [text & codes]
  (if (and *colored-output* (not (empty? codes)))
    [:span [:pass (ansi/esc codes)] text [:pass (ansi/escape :none)]]
    text))


(defn- delimiter
  "Colors a delimiter apropriately."
  [delim]
  (color-text delim :bold :red))



;; DISPATCH MULTIMETHOD

(defn- canonize-dispatch
  [value]
  (if (extends? data/TaggedValue (class value))
    :tagged-value
    (type value)))


(defmulti canonize
  "Converts the given value into a 'canonical' structured document, suitable
  for printing with fipp. This method also supports ANSI color escapes for
  syntax highlighting if desired."
  #'canonize-dispatch)


(defmacro ^:private color-primitive
  [dispatch & codes]
  `(defmethod canonize ~dispatch
     [value#]
     (color-text (pr-str value#) ~@codes)))


(color-primitive nil :bold :black)
(color-primitive java.lang.Boolean :green)
(color-primitive java.lang.Number :cyan)
(color-primitive java.lang.Character :bold :magenta)
(color-primitive java.lang.String :bold :magenta)
(color-primitive clojure.lang.Keyword :bold :yellow)
(color-primitive clojure.lang.Symbol)


(defmethod canonize clojure.lang.ISeq
  [s]
  (let [elements (if (symbol? (first s))
                   (cons (color-text (str (first s)) :bold :blue)
                         (map canonize (rest s)))
                   (map canonize s))]
    [:group
     (delimiter "(")
     [:align (interpose :line elements)]
     (delimiter ")")]))


(defmethod canonize clojure.lang.IPersistentVector
  [v]
  [:group
   (delimiter "[")
   [:align (interpose :line (map canonize v))]
   (delimiter "]")])


(defmethod canonize clojure.lang.IPersistentSet
  [s]
  (let [entries (sort data/total-order (seq s))]
    [:group
     (delimiter "#{")
     [:align (interpose :line (map canonize entries))]
     (delimiter "}")]))


(defn- canonize-map
  [m]
  (let [entries (sort-by first data/total-order (seq m))
        entries (for [[k v] entries]
                  [:span (canonize k) " " (canonize v)])]
    [:group
     (delimiter "{")
     [:align (interpose [:span "," :line] entries)]
     (delimiter "}")]))


(defmethod canonize clojure.lang.IPersistentMap
  [m]
  (canonize-map m))


(defmethod canonize clojure.lang.IRecord
  [r]
  [:span (delimiter "#") (-> r class .getName) (canonize-map r)])


(prefer-method canonize clojure.lang.IRecord clojure.lang.IPersistentMap)


(defmethod canonize :tagged-value
  [v]
  [:span (color-text (str \# (data/tag v)) :red) " " (canonize (data/value v))])


(defmethod canonize :default
  [value]
  (if *strict-mode*
    (throw (IllegalArgumentException.
             (str "No canonical representation for " (class value) ": " value)))
    [:span (color-text "#<" :blue)
     (color-text (.getName (class value)) :bold :blue)
     " " (str value)
     (color-text ">" :blue)]))


(defprinter cprint canonize {:width 80})
