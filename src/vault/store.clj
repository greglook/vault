(ns vault.store
  (:require [clojure.string :as string]
            ))



(defprotocol BlobEncoder
  "Protocol for encoders which process blob content on the way to and from
  storage."

  (encode-stream
    [this output-stream]
    "Wrap the output stream with an encoding stream.")
  (decode-stream
    [this input-stream]
    "Wrap the input stream with a decoding stream."))



