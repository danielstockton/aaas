(ns aaas.db
  (:import datomic.Util)
  (:require [buddy.hashers :as hs]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defrecord Database [uri conn]
  component/Lifecycle
  (start [component]
    (let [db   (d/create-database uri)
          conn (d/connect uri)
          schema (first (Util/readAll (io/reader (io/resource "data/schema.edn"))))
          initial-data (first (Util/readAll (io/reader (io/resource "data/initial.edn"))))]
      @(d/transact conn schema)
      @(d/transact conn initial-data)
      (assoc component :conn conn)))
  (stop [component]
    (when conn (d/release conn))
    (assoc component :conn nil)))

(defn new-database [uri]
  (map->Database {:uri uri}))
