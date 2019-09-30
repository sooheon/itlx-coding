(ns codingtest.crawler
  (:require
   [clj-http.client :as client]
   [jsonista.core :as j]
   [cemerick.url :refer [url url-encode]]
   [codingtest.utils :as u]
   [clojure.set :as set])
  (:import (java.net MalformedURLException)))


(def root (url "https://tyruiop.org/crawler/"))

;;;;;;;;;;;;;;;;;;;;
;; Crawler
;;;;;;;;;;;;;;;;;;;;

(defn- valid-url? [s]
  (boolean (try
             (url s)
             (catch MalformedURLException _
               false))))

(defn- make-url [rel-path]
  (if (valid-url? rel-path)
    rel-path
    (str (url root rel-path))))

(def GET
  (memoize
   (fn [rel-path]
     (client/get (make-url rel-path) {:throw-exceptions false
                                      :ignore-unknown-host true}))))

(defn parse-json
  "Parses json body, fixing trailing commas."
  [json-str]
  (-> json-str
      u/json-remove-trailing-comma
      (j/read-value (j/object-mapper {:decode-key-fn true}))))

(defn parse-page
  "Given relative path fragment, appends it to root to create valid URL (or
   uses it directly). Parses the HTTP response into map with [:"
  [rel-path]
  (let [url (make-url rel-path)
        resp (GET rel-path)
        status (:status resp)]
    (case status
      200 (let [json (-> resp :body parse-json)]
            {:page/url url :response/status status
             :page/links (:links json)
             :page/title (:title json)})
      {:page/url url :response/status status})))

(defn itinerary
  "Given atoms holding current list of hits and parsed-pages, return coll of
   urls to visit next."
  [hits parsed-pages]
  (set/difference (set (mapcat :page/links @parsed-pages))
                  (set @hits)))

(defn- visit-page [hits parsed-pages rel-path]
  (swap! hits conj rel-path)
  (swap! parsed-pages conj (parse-page rel-path)))

(defn visit-all-pages!
  "Given starting relative path (i.e. 'index.json'), visits all linked URLs.
   Returns a collection of URL hits, and a set of parsed pages."
  [start]
  (let [hits* (atom [start])
        parsed-pages* (atom #{(parse-page start)})]
    (loop [to-visit (itinerary hits* parsed-pages*)]
      (when-not (empty? to-visit)
        (u/pmap! (partial visit-page hits* parsed-pages*) to-visit)
        (recur (itinerary hits* parsed-pages*))))
    {:hits @hits*
     :parsed @parsed-pages*}))

(defn count-hits
  "Given list of parsed paged, counts up hits for each url."
  [parsed-pages]
  (reduce
   (fn [acc {:keys [page/links]}]
     (reduce (fn [m link]
               (update m (make-url link) (fnil inc 0)))
             acc
             links))
   {}
   parsed-pages))
