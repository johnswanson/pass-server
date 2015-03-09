(ns user
  (:require [clojure.java.io :as io]
            [clojure.java.javadoc :refer [javadoc]]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [com.stuartsierra.component :as component]
            [pw.core :as core]))

(def system nil)

(defn init
  []
  (alter-var-root #'system (constantly (core/system {}))))

(defn start
  []
  (alter-var-root #'system component/start))

(defn stop
  []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn go
  []
  (init)
  (start)
  :ready)

(defn reset
  []
  (stop)
  (refresh :after 'user/go))
