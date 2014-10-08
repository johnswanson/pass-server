(defproject pw "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]]}}
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.0.3"]]
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [om "0.7.3"]
                 [sablono "0.2.22"]
                 [hiccup "1.0.5"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [fogus/ring-edn "0.2.0"]
                 [environ "1.0.0"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring-server "0.3.1"]
                 [compojure "1.1.6"]])
