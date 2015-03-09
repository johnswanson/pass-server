(ns pw.core
  (:gen-class :implements [org.apache.commons.daemon.Daemon])
  (:import [org.apache.commons.daemon Daemon DaemonContext])
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.cli :refer [parse-opts]]
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
    [:link {:href "/css/main.css"
            :rel "stylesheet"
            :type "text/css"
            :media "all"}]
    [:title "pw"]]
   [:body
    [:div#app]
    (if (:is-production env)
      [:script {:src "/js/out/app.min.js"}]
      (html
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
   (POST ["/pass/:service" :service #"[-\w\/]+"] [service password]
         (if-let [[pw] (get-password path service password)]
           (edn-response {:pass pw})
           (edn-response {:error "Invalid service or password"})))
   (not-found "404")))

(defn my-handler [component password-store]
  (-> (my-routes component password-store)
      (wrap-edn-params)))

(defrecord Webserver [config server]
  component/Lifecycle
  (start [component]
    (if-let [password-store (:store config)]
      (let [server (run-jetty (my-handler component password-store)
                              (assoc config :join? false))]
        (assoc component :server server))
      (throw (Throwable. "Undefined store"))))
  (stop [component]
    (if (:server component) (.stop (:server component)))
    (assoc component :server nil)))

(defn new-webserver [config]
  (map->Webserver {:config config}))

(def webserver-config
  {:port (Integer. (or (env :pw-web-port) "8080"))
   :host (or (env :pw-web-host) "localhost")
   :store (env :pw-web-store)})

(defn system [config]
  (component/system-map
   :server (new-webserver (merge config webserver-config))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default (webserver-config :port)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000)]]
   ["-s" "--store STORE" "Password store"
    :default (webserver-config :store)]
   ["-h" "--host HOST" "Hostname"
    :default (webserver-config :host)]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage [options-summary]
  (->> ["API Server"
        ""
        "Usage: lein run [options]"
        ""
        "Options:"
        options-summary]
       (clojure.string/join "\n")))

(defn error-msg [errors]
  (str "The following errors occurred when parsing your command:\n\n"
       (clojure.string/join "\n" errors)))

(def state (atom {}))

(defn init [{:keys [options errors summary]}]
  (cond
    (:help options) (exit 0 (usage summary))
    errors (exit 1 (error-msg errors)))
  (swap! state assoc :options options))

(defn start []
  (let [sys (system (:options @state))]
    (swap! state assoc :system sys)
    (component/start sys)))

(defn stop []
  (component/stop (:system @state)))

(defn -init [this ^DaemonContext context]
  (print "init")
  (init (parse-opts (.getArguments context) cli-options)))

(defn -start [this]
  (future (start)))

(defn -stop [this]
  (stop))

(defn -destroy [this] nil)

(defn -main
  [& args]
  (init (parse-opts args cli-options))
  (start))

