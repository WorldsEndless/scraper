(ns scraper.ldscatalog
    (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html])
    (:import [java.net URL]
             [java.io File]))

(def catalog-url "http://tech.lds.org/glweb/?action=catalog.query&languageid=1&platformid=1&format=json")

(defn get-catalog-text
  [catalog-url]
  (let [json (-> (client/get catalog-url) :body json/parse-string)] ;(def json (-> (client/get catalog-url) :body json/parse-string))
    (-> json
        (get "catalog")
        (->> (spit "/home/torysa/tmp/ldscat.json")))))

;(get-catalog-text catalog-url)
