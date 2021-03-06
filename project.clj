(defproject build-mon "0.1.0-SNAPSHOT"
  :description "A simple build monitor to monitor Visual Studio Online builds"
  :url "localhost:3000"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-codec "1.0.1"]
                 [bidi "1.24.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.5.0"]
                 [hiccup "1.0.5"]]
  :main ^:skip-aot build-mon.core
  :resource-paths ["resources"]
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[midje "1.8.2"]]
                   :plugins  [[lein-midje "3.2"]]}
             :uberjar {:aot :all}})
