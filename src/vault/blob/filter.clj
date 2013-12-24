(ns vault.blob.filter)


(defprotocol BlobCodec
  "Protocol for filters which process blob content on the way to and from
  storage."

  (wrap-encoder
    [this status output-stream]
    "Wrap the output stream with an encoding stream. Returns a vector of the
    updated status map and wrapped output stream.")

  (wrap-decoder
    [this status input-stream]
    "Wrap the input stream with a decoding stream. Returns a vector of the
    updated status map and wrapped output stream."))


(defn encode-stream
  "Returns a new output stream wrapped with the given encoders. Data written to
  the stream will be processed by each codec in the order given. Returns a
  vector of the updated status map and the fully-encoded output stream."
  [status output-stream codecs]
  (let [wrap (fn [[status stream] codec]
               (wrap-encoder codec status stream))]
    (reduce wrap [status output-stream] (reverse codecs))))


(defn decode-stream
  "Returns a new input stream wrapped with the given decoders. Data read from
  the stream will be processed by each codec in the reverse of the order given,
  so that the same sequence of codecs will work for both `encode-stream` and
  `decode-stream`. Returns a vector of the updated status map and the
  fully-decoded input stream."
  [status input-stream codecs]
  (let [unwrap (fn [[status stream] codec]
                 (wrap-decoder codec status stream))]
    (reduce unwrap [status input-stream] (reverse codecs))))
