(ns vault.print
  "Utilities for canonical printing of EDN values."
  (:require (clojure [string :as string])
            [vault.print.ansi :as ansi]
            [fipp.printer :refer [defprinter]]))


;; HELPER FUNCTIONS

(def ^:dynamic *colored-output*
  "Output ANSI colored output from print functions."
  false)


(defmacro with-colored-output
  "Performs the given forms with colored output enabled."
  [& body]
  `(binding [*colored-output* true]
    ~@body))


(defn- color-text
  "Constructs a text doc, which may be colored if *colored-output* is true."
  [text & codes]
  (if *colored-output*
    [:span [:pass (apply ansi/escape codes)] text [:pass (ansi/escape :none)]]
    text))


(defn- delimiter
  "Colors a delimiter apropriately."
  [delim]
  (color-text delim :bold :red))



;; DISPATCH MULTIMETHOD

(defmulti canonize
  "Converts the given value into a 'canonical' structured document, suitable
  for printing with fipp. This method also supports ANSI color escapes for
  syntax highlighting if desired."
  type)


(defmacro ^:private color-primitive
  [dispatch & codes]
  `(defmethod canonize ~dispatch
     [value#]
     (color-text (pr-str value#) ~@codes)))


(color-primitive nil :red)
(color-primitive java.lang.Boolean :bold :blue)
(color-primitive java.lang.Number :cyan)
(color-primitive java.lang.Character :bold :magenta)
(color-primitive java.lang.String :bold :magenta)
(color-primitive clojure.lang.Keyword :bold :yellow)


(defmethod canonize clojure.lang.ISeq
  [s]
  [:group
   (delimiter "(")
   [:align (interpose :line (map canonize s))]
   (delimiter ")")])


; vector

; Maps should print their entries in sorted key order.

; Canonical sets should print their entries in sorted order.



(defmethod canonize :default
  [value]
  [:text (str \< (.getName (class value)) \space (pr-str value) \>)])


(defprinter cprint canonize {:width 80})
