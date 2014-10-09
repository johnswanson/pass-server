(ns pw.main
  (:gen-class)
  (:require [pw.core :refer [system]]
            [com.stuartsierra.component :as component]))

(defn -main []
  (component/start (system)))
