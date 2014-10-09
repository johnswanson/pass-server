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
    (if (:production? env)
      [:script {:src "/js/out/app.js"}]
      (html
       [:script {:src "/js/react.js"}]
       [:script {:src "/js/out/goog/base.js"}]
       [:script {:src "/js/out/app.js"}]
       [:script "goog.require('pw.core');"]))]))

(defn edn-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn my-routes [component path]
  (routes
   (resources "/")
   (GET "/" [] (index))
   (POST "/pass/*" [* password]
          (let [{:keys [out err]} (sh "gpg"
                        "--batch"
                        "-d"
                        "--passphrase-fd" "0"
                        (format "%s/%s.gpg" path *)
                        :in password)]
            (cond
             (re-find #"No such file or directory" err)
             (edn-response {:error "Bad service or password"})

             (= "" out)
             (edn-response {:error "Bad service or password"})

             :else
             (edn-response {:pass (clojure.string/trim-newline out)}))))

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

(defn system []
  (let [{:keys [server storage]} env]
    (component/system-map
     :server (new-webserver server))))

