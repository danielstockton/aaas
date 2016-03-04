(defproject aaas "0.1.0-SNAPSHOT"
  :description "Auth as a service"
  :dependencies [[bidi "2.0.1"]
                 [buddy "0.10.0"]
                 [clj-http "2.1.0"]
                 [clj-time "0.11.0"]
                 [com.datomic/datomic-free "0.9.5350"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.taoensso/timbre "4.3.1"]
                 [org.clojure/clojure "1.8.0"]
                 [ring-middleware-format "0.7.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :main aaas.core
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :repl-options {:init-ns user}
                   :resource-paths ["config"]
                   :source-paths ["dev"]}
             :uberjar {:aot :all}})
