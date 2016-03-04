(ns aaas.util
  (:require [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream]))

(defn read-transit [s]
  (let [in (ByteArrayInputStream. (.getBytes s))]
    (transit/read (transit/reader in :json))))
