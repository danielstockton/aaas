(ns user
  (:require [aaas.core :as core]
            [clojure.edn :as edn]
            [clojure.tools.namespace.repl :refer (refresh)]
            [com.stuartsierra.component :as component]))

(defonce system nil)

(defn init []
  (alter-var-root #'system
                  (constantly (core/system (core/config)))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(comment
  (go)

  (reset)

  )
