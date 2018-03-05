(ns scraper.npr
    (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]
            [pl.danieljanus.tagsoup :as ts])
    (:import [java.net URL]
             [java.io File]))

(defn extract-stories [archive-html]
  (let [raw-story-entries (-> archive-html first :content)
        story-entries (filter (fn [i] (instance? clojure.lang.PersistentStructMap i)) raw-story-entries)
        entries-info (for [story story-entries] (-> story :attrs (select-keys [:data-episode-date :data-episode-id])))]
    entries-info))

(defn gen-date-url [story-info] 
                   (let [url-base "http://www.npr.org/programs/all-things-considered/"
                         ded (:data-episode-date story-info)
                         date-string (clojure.string/replace
                                      ded
                                      "-"
                                      "/")]
                     (str url-base date-string "/" (:data-episode-id story-info) "?showDate=" ded)))

(defn gen-story-urls [date-url]
                   (let [stories (-> date-url URL. html/html-resource (html/select [:article :h1 :a]))]
                     (map #(-> % :attrs :href) stories)))

(defn get-story-text
  "Get the transcript text of a story at a url"
  [story-url]
  (let [transcript (-> story-url URL. html/html-resource (html/select [:div.transcript.storytext :p]))
        smap (map #(-> % :content first) (drop-last 2 transcript))]
    (clojure.string/join "\n\n" smap)))

(defn get-allthingsconsidered []
  (let [archive-url "https://www.npr.org/programs/all-things-considered/archive" ; This will be offline saved html
            ;;; -- Whole archive, needs to be browser-viewed to the end
        ;; https://www.npr.org/programs/all-things-considered/archive?date=2018-01-27&eid=581277278
        ;; https://www.npr.org/programs/all-things-considered/archive?date=2018-01-22&eid=579629874
        ;archive-url "file:///home/torysa/Downloads/AllThingsConsidered.html"
        html-base (-> archive-url
                      URL.
                      html/html-resource)
        initial-archives (html/select html-base [:#episodelist :.episodeArchiveWrapper])
        initial-stories-info (extract-stories initial-archives)
        infinite-stories (html/select html-base [:#infinitescroll])
        infinite-stories-info (extract-stories infinite-stories)
        all-stories-info (concat initial-stories-info infinite-stories-info)
        ;; with entries-info, for each one, generate/parse the transcript URL and get the content
        all-dates-urls (map gen-date-url all-stories-info)
        all-story-urls (map gen-story-urls all-dates-urls)
        ;; now I need each article name, to generate the next URL
        ]
    ;; now lets write them to files.
    (doseq [surl (flatten all-story-urls)]
      (let [item-name (-> surl (clojure.string/split #"/") last)
            out-url (str "/home/torysa/Temp/atc/" item-name ".txt")]
        (println "Writing to " out-url "...")
        (->> surl
             get-story-text
             (spit out-url))))))



;; it takes the data-episode-date and data-episode-id of the last element in the infinite scroll and requests the next from there
;; 
;;https://www.npr.org/programs/all-things-considered/archive?date=2018-01-02&eid=575030397

;; (def u2 (-> "file:///home/torysa/Downloads/AllThingsConsidered.html" URL. .openConnection))
;; #'scraper.npr/u2
;; scraper.npr> (.getContentType u2)
;; text/html
;; scraper.npr> 
