(ns vault.blob.filter)


(defprotocol BlobCodec
  "Protocol for filters which process blob content on the way to and from
  storage."

  (wrap-output
    [this stream]
    "Wrap the output stream with an encoding stream. Returns a vector of
    additional status metadata and the wrapped output stream.")

  (wrap-input
    [this status stream]
    "Wrap the input stream with a decoding stream. Returns the wrapped input
    stream."))


(defn select-codecs
  "Utility function to select a sequence of codecs from a map of available
  implementations. Throws an exception if one of the specified codecs is not
  present."
  [codec-map names]
  (let [missing (->> names
                     (map #(if-not (codecs %) %))
                     (remove nil?))]
    (when (seq missing)
      (throw (RuntimeException.
               (str "Unsupported blob codecs: " missing)))))
  (map codec-map names))


(defn encode-output
  "Returns a new output stream wrapped with the given encoders. Data written to
  the stream will be processed by each codec in the order given. Returns a
  vector of status metadata and the fully-encoded output stream."
  [codec-map names stream]
  (let [codecs (select-codecs codec-map names)
        wrap (fn [[status stream] codec]
               (let [[codec-status stream] (wrap-output codec stream)
                     status (merge status codec-status)]
                 [status stream]))
        [status stream] (reduce wrap [{} output-stream] (reverse codecs))
        status (assoc status :codecs names)]
    [status stream]))


(defn decode-input
  "Returns a new input stream wrapped with the given decoders. Data read from
  the stream will be processed by each codec in the reverse of the order given,
  so that the same sequence of codecs will work for both `encode-stream` and
  `decode-stream`. Returns the fully-decoded input stream."
  [codec-map status stream]
  (let [names (:codecs status)
        codecs (select-codecs codec-map names)
        unwrap (fn [stream codec]
                 (wrap-input codec status stream))]
    (reduce unwrap stream (reverse codecs))))
