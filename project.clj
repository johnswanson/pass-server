(defproject pw "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.0.3"]]
  :source-paths ["src/cljs" "src/clj"]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/out/app.js"
                           :output-dir "resources/public/js/out"
                           :optimizations :none
                           :pretty-print true
                           :source-map true}}
               {:id "prod"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/out/app.js"
                           :optimizations :advanced
                           :pretty-print false
                           :preamble ["react/react.min.js"]
                           :externs ["react/externs/react.js"]
                           :jar true}}]}
  :main pw.main
  :aot [pw.main]
  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "0.0-2356"]
                 [com.stuartsierra/component "0.2.2"]
                 [om "0.7.3"]
                 [sablono "0.2.22"]
                 [hiccup "1.0.5"]
                 [fogus/ring-edn "0.2.0"]
                 [environ "1.0.0"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [ring-server "0.3.1"]
                 [compojure "1.2.0"]])
