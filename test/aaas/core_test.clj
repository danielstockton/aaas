(ns aaas.core-test
  (:require [aaas.core :as core]
            [aaas.util :refer [read-transit]]
            [buddy.sign.jws :as jws]
            [clj-http.client :as client]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]))

(defonce system (core/system {:port 3001
                              :secret "pass"
                              :db-uri "datomic:mem://localhost:4334/test-aaas"}))

(component/start system)

(def request-defaults {:throw-exceptions false
                       :content-type :transit+json})

(deftest test-login
  (testing "Bad credentials"
    (let [params (merge request-defaults {:form-params {:email "demo@aaas.com"
                                                        :password "wrongpass"}})
          res    (client/post "http://localhost:3001/login" params)
          body   (:body res)]
      (is (= body "Unauthorized"))))
  (testing "Good credentials"
    (let [params (merge request-defaults {:form-params {:email    "demo@aaas.com"
                                                        :password "pass"}})
          res    (client/post "http://localhost:3001/login" params)
          token  (:token (read-transit (:body res)))
          pubkey (get-in system [:web-server :pubkey])]
      (is (= (get-in (jws/unsign token pubkey core/sign-opts) [:user :user/email])
             "demo@aaas.com")))))

(deftest test-logout
  (let [res (client/get "http://localhost:3001/logout")]
    (is (= (:status res) 204))
    (is (nil? (:body res)))))
