(ns vault.print.ansi
  (:require [clojure.string :as string]))


(def sgr-code
  "Map of symbols to numeric SGR (select graphic rendition) codes."
  {:none        0
   :bold        1
   :underline   3
   :blink       5
   :reverse     7
   :hidden      8
   :strike      9
   :black      30
   :red        31
   :green      32
   :yellow     33
   :blue       34
   :magenta    35
   :cyan       36
   :white      37
   :fg-256     38
   :fg-reset   39
   :bg-black   40
   :bg-red     41
   :bg-green   42
   :bg-yellow  43
   :bg-blue    44
   :bg-magenta 45
   :bg-cyan    46
   :bg-white   47
   :bg-256     48
   :bg-reset   49})


(defn escape
  "Returns an ANSI escope string which will enact the given SGR codes."
  [& codes]
  (let [codes (map sgr-code codes codes)]
    (str \u001b \[ (string/join \; codes) \m)))


(defn sgr
  "Wraps the given string with SGR escape codes."
  [string & codes]
  (str (apply escape codes) string (escape :none)))


(defmacro with-sgr
  "Takes a sequence of SGR keys or codes, followed by a body of forms which
  print out some text to be colored."
  [& args]
  (let [[codes forms] (split-with #(or (keyword? %) (number? %)) args)]
    `(do (print (escape ~@codes))
         ~@forms
         (print (escape :none)))))
