(ns aaas.core
  (:gen-class)
  (:require [aaas.db :as db]
            [bidi.bidi :as bidi]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.core.keys :as ks]
            [buddy.hashers :as hs]
            [buddy.sign.jws :as jws]
            [clj-time.core :as time]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.util.response :refer [response status]]
            [taoensso.timbre :as log]))

(def sign-opts {:alg :ps256})

(defn claims [user]
  (let [public-user (dissoc user :user/password)]
    (assoc {:exp (time/plus (time/now) (time/seconds 5))} :user public-user)))

(defn create-token [user privkey]
  (jws/sign (claims user) privkey sign-opts))

(defn login
  [req]
  (let [email    (get-in req [:params :email])
        password (get-in req [:params :password])
        user     (d/pull @(d/sync (:conn req)) '[*] [:user/email email])
        valid?   (hs/check password (:user/password user))]
    (if valid?
      (let [token (create-token user (:privkey req))]
        (response {:token token}))
      (throw-unauthorized))))

(defn logout [req]
  (-> (response nil) (status 204)))

(defn session [req]
  (if (authenticated? req)
    (let [user (:identity req)]
      (response user))
    (throw-unauthorized)))

(defn refresh [req]
  (if (authenticated? req)
    (let [user  (:identity req)
          token (create-token user (:privkey req))]
      (response {:token token}))
    (throw-unauthorized)))

(defn not-found [req]
  (status (response "") 404))

(defn backend [pubkey]
  (jws-backend {:options    sign-opts
                :secret     pubkey
                :token-name "Bearer"}))

(def routes
  ["" {"/login"   :login
       "/logout"  :logout
       "/session" :session
       "/refresh" :refresh
       true       :not-found}])

(defn app-routes [req]
  (let [match (bidi/match-route
               routes
               (:uri req)
               :request-method (:request-method req))]
    (case (:handler match)
      :login     (login req)
      :logout    (logout req)
      :session   (session req)
      :refresh   (refresh req)
      :not-found (not-found req)
      req)))

(defn wrap-privkey [handler privkey]
  (fn [req] (handler (assoc req :privkey privkey))))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :conn conn))))

(defn handler [privkey pubkey conn]
  (-> app-routes
      (wrap-privkey privkey)
      (wrap-connection conn)
      (wrap-authorization (backend pubkey))
      (wrap-authentication (backend pubkey))
      (wrap-restful-format :formats [:json-kw :transit-json])))

(defrecord WebServer [options server handler privkey pubkey db]
  component/Lifecycle
  (start [component]
    (when-not server
      (log/info "Starting web server")
      (let [conn (:conn db)
            server (jetty/run-jetty (handler privkey pubkey conn) options)]
        (assoc component :server server))))
  (stop [component]
    (when server
      (log/info "Stopping web server")
      (.stop server)
      component)))

(defn new-web-server
  ([port handler privkey pubkey]
   (new-web-server port handler privkey pubkey {}))
  ([port handler privkey pubkey options]
   (map->WebServer {:options (merge {:port port :join? false} options)
                    :handler handler
                    :privkey privkey
                    :pubkey  pubkey})))

(defn system [config]
  (let [{:keys [port secret db-uri]} config
        privkey (ks/private-key (io/resource "privkey.pem") secret)
        pubkey  (ks/public-key (io/resource "pubkey.pem"))]
    (-> (component/system-map
         :db (db/new-database db-uri)
         :web-server (component/using
                      (new-web-server port handler privkey pubkey)
                      {:db :db})))))

(defn config []
  (edn/read-string (slurp (io/resource "config.edn"))))

(defn -main [& args]
  (component/start
     (system (config))))
