(ns vault.util.io
  (:require
    byte-streams))


(def ^:dynamic *buffer-size*
  "Size of buffer to use in data functions."
  1024)


(defn apply-bytes
  "Calls the given function on chunks of the byte sequence read from the given
  data source. The function should accept a byte array and a number of bytes to
  use from it."
  [source f]
  (with-open [stream (byte-streams/to-input-stream source)]
    (let [buffer (byte-array *buffer-size*)]
      (loop [n (.read stream buffer)]
        (when (pos? n)
          (f buffer n)
          (recur (.read stream buffer)))))))


(defmacro do-bytes
  "Wraps the given statements in a function to pass to apply-bytes."
  [source [buff-sym n-sym] & body]
  `(let [f# (fn [~(vary-meta buff-sym assoc :tag 'bytes)
                 ~(vary-meta n-sym assoc :tag 'long)]
              ~@body)]
     (apply-bytes ~source f#)))
