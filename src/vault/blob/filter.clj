(ns vault.blob.filter)


(defprotocol BlobCodec
  "Protocol for filters which process blob content on the way to and from
  storage."

  (codec-key
    [this]
    "Return the keyword which represents this codec.")

  (wrap-encoder
    [this output-stream]
    "Wrap the output stream with an encoding stream.")

  (wrap-decoder
    [this input-stream]
    "Wrap the input stream with a decoding stream."))


(defn encode-stream
  "Returns a new output stream wrapped with the given encoders. Data written to
  the stream will be processed by each codec in the order given."
  [output-stream & encoders]
  (reduce #(wrap-encoder %2 %1) output-stream (reverse encoders)))


(defn decode-stream
  "Returns a new input stream wrapped with the given decoders. Data read from
  the stream will be processed by each codec in the reverse of the order given,
  so that the same sequence of codecs will work for both `encode-stream` and
  `decode-stream`."
  [input-stream & decoders]
  (reduce #(wrap-decoder %2 %1) input-stream (reverse decoders)))
