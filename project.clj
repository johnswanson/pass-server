(defproject pw "0.1.0-SNAPSHOT"
  :description "A simple server for the unix password manager, pass"
  :url "http://github.com/johnswanson/pass-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.0.3"]]
  :source-paths ["src/cljs" "src/clj"]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :compiler {:main pw.core
                           :output-to "resources/public/js/out/app.js"
                           :output-dir "resources/public/js/out"
                           :optimizations :whitespace
                           :pretty-print true
                           :source-map "resources/public/js/out/app.js.map"}}
               {:id "prod"
                :source-paths ["src/cljs"]
                :compiler {:main pw.core
                           :output-to "resources/public/js/out/app.min.js"
                           :optimizations :advanced
                           :pretty-print false}}]}
  :main ^:skip-aot pw.core
  :profiles {:uberjar {:aot :all}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "0.0-2913"]
                 [org.omcljs/om "0.8.8"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.apache.commons/commons-daemon "1.0.9"]
                 [com.stuartsierra/component "0.2.2"]
                 [sablono "0.2.22"]
                 [hiccup "1.0.5"]
                 [fogus/ring-edn "0.2.0"]
                 [environ "1.0.0"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [ring-server "0.3.1"]
                 [compojure "1.2.0"]])
