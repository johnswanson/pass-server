(ns pw.core
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.params]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.session :as session]
            [clojure.java.shell :refer [sh]]
            [environ.core :refer [env]]))

(defn my-routes [{:keys []}]
  (routes
   (resources "/")
   (POST "/pass/*" [* test]
        (let [out (sh "gpg"
                      "--batch"
                      "-d"
                      "--passphrase-fd" "0"
                      (format "/home/jds/.password-store/%s.gpg" *)
                      :in test)]
          (cond
           (re-find #"No such file or directory" (:err out)) nil
           (= "" (:out out)) (str "failed decryption: " test)
           :else (:out out))))
   (not-found "404")))

(defn my-handler [{storage :storage :as component}]
  (-> (my-routes component)))

(defrecord Webserver [config server]
  component/Lifecycle
  (start [component]
    (let [server (run-jetty (my-handler component) (assoc config :join? false))]
      (assoc component :server server)))
  (stop [component]
    (if (:server component) (.stop (:server component)))
    (assoc component :server nil)))

(defn new-webserver [config]
  (map->Webserver {:config config}))

(defn system []
  (let [{:keys [server storage]} env]
    (component/system-map
     :server (new-webserver server))))

