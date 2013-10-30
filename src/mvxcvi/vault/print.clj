(ns mvxcvi.vault.print
  "Utilities for canonical printing of EDN values."
  (:require (clojure [pprint :refer :all]
                     [string :as string])
            [mvxcvi.vault.print.ansi :as ansi]
            #_ [fipp.edn :refer [pprint]]))


;; HELPER FUNCTIONS

(def ^:dynamic *colored-output*
  "Output ANSI colored output from print functions."
  false)


(defmacro with-colored-output
  "Performs the given forms with colored output enabled."
  [& body]
  `(binding [*colored-output* true]
    ~@body))


(defn- color
  "Wrap color codes around a string if *colored-output* is set."
  [string & codes]
  (if *colored-output*
    (apply ansi/sgr string codes)
    string))


(defn- pr-colored
  "Print a colored version of the printed output of the value."
  [value & codes]
  (let [text (with-out-str (print-method value *out*))
        text (apply color text codes)]
    (.write ^java.io.Writer *out* text)))


(defmacro ^:private colored-dispatch
  [dispatch-value & codes]
  `(defmethod canonical-dispatch ~dispatch-value
     [value#]
     (pr-colored value# ~@codes)))



;; DISPATCH MULTIMETHOD

(defmulti canonical-dispatch
  "Pretty-printer dispatch function for canonical EDN printing. This method also
  supports ANSI color escapes for syntax highlighting if desired."
  type)


(defmethod canonical-dispatch clojure.lang.ISeq
  [alist]
  (pprint-logical-block
    :prefix (color "(" :bold :red)
    :suffix (color ")" :bold :red)
    (print-length-loop [alist (seq alist)]
      (when alist
        (write-out (first alist))
        (when (next alist)
          (.write ^java.io.Writer *out* " ")
          (pprint-newline :linear)
          (recur (next alist)))))))


; vector

; Maps should print their entries in sorted key order.

; Canonical sets should print their entries in sorted order.

(colored-dispatch clojure.lang.Keyword :bold :yellow)
(colored-dispatch java.lang.Boolean :bold :blue)
(colored-dispatch java.lang.Number :cyan)
(colored-dispatch java.lang.Character :bold :magenta)
(colored-dispatch java.lang.String :bold :magenta)


; nil/true/false


(defmethod canonical-dispatch :default
  [value]
  (.write ^java.io.Writer *out* (str \< (.getName (class value)) \space))
  (pr value)
  (.write ^java.io.Writer *out* ">"))


(defn cprint
  "Canonically-print a value."
  [value]
  (with-pprint-dispatch canonical-dispatch
    (pprint value)))
