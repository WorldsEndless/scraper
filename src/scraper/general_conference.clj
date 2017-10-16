(ns scraper.general-conference
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
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
  (let [archive-url "http://www.npr.org/programs/all-things-considered/archive" ; This will be offline saved html
            ;;; -- Whole archive, needs to be browser-viewed to the end
        archive-url "file:///home/torysa/Downloads/AllThingsConsidered.html"
        html-base (-> archive-url URL. html/html-resource)
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



;; Care of /home/torysa/Workspace/Clojure/OthersPrograms/mastering_clojure_data_analysis_code/4139OS_03_code/tm-sotu/src/tm_sotu/download.clj
(defn gen-catalog-url [dept program]
  (let [base "http://catalog.byu.edu/humanities/"]
    (str base dept "/" program)))

(defn get-requirements [dept program]
  (let [url (gen-catalog-url dept program)
        selector [:#main :#content]
        cut-out [:div.field-name-field-program-outcomes]]
    (-> url
        URL.
        html/html-resource
        (html/select selector)
        ;; TODO remove the "program outcomes" section
        (html/transform cut-out (html/substitute nil))
        html/emit* ; to strings
        (->> (apply str)) ; strings -> one string
        (clojure.string/replace "href=\"/" "href=\"http://catalog.byu.edu/" ) ; fix links
        )))

(defn get-title [talk]
  (-> (html/select talk [:div.title-block :div]) first :content first))

(defn get-author [talk]
  (-> (html/select talk [:a.article-author__name]) first :content first))

(defn get-content [talk] (html/select talk [:div.body-block]))

(defn get-references [talk] (html/select talk [:footer.notes :ol]))

(defn make-filename [name]
  (-> name
      (clojure.string/replace #" " "-")
      (clojure.string/replace #"[^a-zA-Z-]" "")))

(defn gc [& [dirpath]]
  (let [dirpath (or dirpath "/home/torysa/Documents/Gospel_Files/General_Conference/20172")
        talks (filter (fn [file]
                        (and
                         (.isFile file)
                         (re-matches #"[^.]+" (str file))))
                      (-> dirpath io/file file-seq sort))
        html-talks (map html/html-resource talks)
        output-path "/home/torysa/Documents/Gospel_Files/General_Conference/20172/org/"
        single-output-file (str output-path "all.org")]
    (println "writing to " single-output-file)
    (doseq [talk html-talks]
      (let [title (get-title talk)
            author (get-author talk)
            enlive-html-content (get-content talk)
            html-content-string (reduce str (html/emit* enlive-html-content))
            html-references-string (reduce str (html/emit* (get-references talk)))
            org-doc (:out
                     (clojure.java.shell/sh "pandoc" "-f" "html" "-t" "org" :in (str
                                                                                 "<h1>" title " (" (subs author 3) ")</h1>"
                                                                                 "<h2>Contents</h2>" html-content-string
                                                                                 "<h2>References</h2>" html-references-string)))
            output-file-name  (->> author
                              make-filename
                              (drop 3)
                              (apply str)
                              ((fn [authstr]
                                 (str
                                  authstr
                                  "-"
                                  (make-filename title)
                                  ".org"))))
            ]
        (do
          (println "Processing " title)
          (spit single-output-file org-doc :append true)
          ;(spit (str output-path output-file-name) org-doc)
          )))))
