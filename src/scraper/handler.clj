(ns scraper.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html])
  (:import [java.net URL]
           [java.io File]))

;; Care of /home/torysa/Workspace/Clojure/OthersPrograms/mastering_clojure_data_analysis_code/4139OS_03_code/tm-sotu/src/tm_sotu/download.clj
(defn gen-catalog-url [dept program]
  (let [base "http://catalog.byu.edu/humanities/"]
    (str base dept "/" program)))

(defn get-requirements [dept program]
  ;; (def dept "comparative-arts-and-letters")
  ;; (def program "classical-studies-classics-ba")
  (def dept "AmerStudies")
  (def program "arabic-minor")
  (-> (gen-catalog-url dept program)
      URL.
      html/html-resource
      (html/select [:#content])
      ;; TODO remove the "program outcomes" section
      (html/transform
       [:div.field-name-field-program-outcomes]
       (html/substitute nil))
      html/emit*
      (->> (apply str))
      (clojure.string/replace "href=\"/" "href=\"http://catalog.byu.edu/")))
;;(gen-catalog-url "comparative-arts-and-letters" "classical-studies-classics-ba")
;;(get-requirements "comparative-arts-and-letters" "classical-studies-classics-ba")

(defroutes app-routes
  (GET "/requirements/:dept/:program" [dept program] (get-requirements dept program))
  ;;http://localhost:3000/requirements/comparative-arts-and-letters/art-history-curatorial-studies-ba
  ;; http://humpre.byu.edu/scraper/requirements/asian-and-near-eastern-languages/arabic-minor
  (GET "/" [] "Hello World")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
