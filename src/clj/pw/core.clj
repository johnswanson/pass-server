(ns pw.core
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.params]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.session :as session]
            [clojure.java.shell :refer [sh]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [environ.core :refer [env]]))

(defn index []
  (html5
   [:head
    [:link {:href "/css/main.css" :rel "stylesheet" :type "text/css" :media "all"}]
    [:title "pw"]]
   [:body
    [:div#app]
    (if (:is-production env)
      [:script {:src "/js/out/app.min.js"}]
      (html
       [:script {:src "https://fb.me/react-0.11.2.js"}]
       [:script {:src "/js/out/goog/base.js"}]
       [:script {:src "/js/out/app.js"}]
       [:script "goog.require('pw.core');"]))]))

(defn edn-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn get-password [path service password]
  (let [{:keys [out err]} (sh "gpg"
                              "--batch"
                              "-d"
                              "--passphrase-fd" "0"
                              (format "%s/%s.gpg" path service)
                              :in password)]
    (if (or (re-find #"No such file or directory" err) (= "" out))
      nil
      (clojure.string/split out #"\n"))))

(defn my-routes [component path]
  (routes
   (resources "/")
   (GET "/" [] (index))
   (POST "/pass/*" [* password]
         (if-let [[pw] (get-password path * password)]
           (edn-response {:pass pw})
           (edn-response {:error "Invalid service or password"})))
   (not-found "404")))

(defn my-handler [component password-store]
  (-> (my-routes component password-store)
      (wrap-edn-params)))

(defrecord Webserver [config server]
  component/Lifecycle
  (start [component]
    (if-let [password-store (:password-store env)]
      (let [server (run-jetty (my-handler component password-store)
                              (assoc config :join? false))]
        (assoc component :server server))
      (throw (Throwable. "env must contain PASSWORD_STORE"))))
  (stop [component]
    (if (:server component) (.stop (:server component)))
    (assoc component :server nil)))

(defn new-webserver [config]
  (map->Webserver {:config config}))

(defn server-config []
  (let [{:keys [server-port server-host]} env
        assoc-port #(if server-port (assoc % :port (Integer. server-port)) %)
        assoc-host #(if server-host (assoc % :host server-host) %)]
    (-> {}
        (assoc-port)
        (assoc-host))))

(defn system []
  (component/system-map
   :server (new-webserver (server-config))))

