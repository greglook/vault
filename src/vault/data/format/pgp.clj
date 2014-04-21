(ns vault.data.format.pgp
  "Functions to read blobs containing pgp data.")


(defn read-blob
  "Reads the contents of the given blob and attempts to parse it as a PGP
  object. Returns an updated blob record, or nil if the content is not PGP
  data."
  [blob]
  nil)
