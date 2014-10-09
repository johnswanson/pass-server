(ns pw.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! >! timeout]]
            [goog.events :as events]
            [goog.dom :as gdom]
            [cljs.reader :as reader]
            [clojure.string]
            [sablono.core :as html :refer-macros [html]])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def meths {:get "get" :put "put" :post "post" :delete "delete"})

(defn edn-xhr [{:keys [method url data channel]}]
  (let [xhr (XhrIo.)
        callback (fn [d] (go (>! channel d)))]
    (events/listen xhr goog.net.EventType.COMPLETE
                   (fn [e]
                     (if (.isSuccess xhr)
                       (callback (reader/read-string
                                  (.getResponseText xhr))))))
    (. xhr
       (send url (meths method) (when data (pr-str data))
             #js {"Content-Type" "application/edn"
                  "Accept" "application/edn"}))))

(defn service [] (.-value (gdom/getElement "service")))
(defn password [] (.-value (gdom/getElement "password")))

(defn fetch-password [state]
  (let [rc (chan)]
    (edn-xhr {:method :post
              :url (str "/pass/" (service))
              :data {:password (password)}
              :channel rc})
    (go (let [fetched (<! rc)]
          (om/transact! state (fn [s] (assoc s :fetched fetched)))))))

(defn output [fetched owner]
  (reify
    om/IRender
    (render [_]
      (when fetched
        (html
         (let [class (if (:error fetched) "error" "success")]
           [:input#result {:value (if (:error fetched)
                                    (:error fetched)
                                    (:pass fetched))
                           :ref "result"
                           :class class}]))))
    om/IDidUpdate
    (did-update [this one two]
      (let [timeout-chan (timeout 3000)]
        (.focus (om/get-node owner "result"))
        (.select (om/get-node owner "result"))
        (go (<! timeout-chan)
            (om/transact! fetched (constantly {:result ""})))))))

(defn fetch-password-if-enter [state evt]
  (when (= (.-keyCode evt) 13)
    (fetch-password state)))

(defn app [state owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:input#service {:type "text"
                         :placeholder "service"
                         :onKeyUp (partial fetch-password-if-enter state)}]
        [:input#password {:type "password"
                          :placeholder "password"
                          :onKeyUp (partial fetch-password-if-enter state)}]
        (om/build output (:fetched state))]))))

(def init-state (atom {}))

(om/root app init-state {:target (gdom/getElement "app")})
