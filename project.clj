(defproject scraper "0.1.0-SNAPSHOT"
  :description "Basic web-scraping for certain BYU services"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.1.5"]
                 [enlive "1.1.6"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-immutant "2.1.0"]]
  :immutant {
             :war {
                   :name "scraper-%v%t"
                   ;; Destination defaults to :target-path
                   ;:destination "/ssh:humpre:/srv/wildfly/"
                   :resource-paths ["war-resources"]
                   :context-path "/"}}
  :ring {:handler scraper.handler/app
         :uberwar-name "scraper.war"}
  
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
