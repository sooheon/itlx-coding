(ns codingtest.crawler
  (:require
   [cemerick.url :refer [url url-encode]]
   [clj-http.client :as client]
   [clojure.set :as set]
   [codingtest.utils :as u]
   [datascript.core :as d]
   [jsonista.core :as j])
  (:import (java.net MalformedURLException)))


(def root (url "https://tyruiop.org/crawler/"))


;;;;;;;;;;;;;;;;;;;;
;; GET and parse
;;;;;;;;;;;;;;;;;;;;

(defn- valid-url? [s]
  (boolean (try
             (url s)
             (catch MalformedURLException _
               false))))

(defn make-url [rel-path]
  (if (valid-url? rel-path)
    rel-path
    (str (url root rel-path))))

(def GET
  (memoize
   (fn [url]
     (client/get url {:throw-exceptions false :ignore-unknown-host true}))))

(defn parse-json
  "Parses json body, fixing trailing commas."
  [json-str]
  (-> json-str
      u/json-remove-trailing-comma
      (j/read-value (j/object-mapper {:decode-key-fn true}))))

(defn parse-page
  "Given URL, parses the HTTP response. Returns map with [:page/url
  :page/title :response/status :page/links]"
  [url]
  (let [resp (GET url)
        status (:status resp)]
    (case status
      200 (let [json (-> resp :body parse-json)]
            {:page/url url :response/status status
             :page/links (map make-url (:links json))
             :page/title (:title json)})
      nil {:page/url url}
      {:page/url url :response/status status})))


;;;;;;;;;;;;;;;;;;;;
;; Crawler strategy
;;;;;;;;;;;;;;;;;;;;

(defn itinerary
  "Given atoms holding current list of hits and parsed-pages, return coll of
   urls to visit next."
  [hits parsed-pages]
  (set/difference (set (mapcat :page/links @parsed-pages))
                  (set @hits)))

(defn- visit-page [hits parsed-pages url]
  (swap! hits conj url)
  (swap! parsed-pages conj (parse-page url)))

(defn visit-all-pages!
  "Given starting URL, visits all linked URLs. Returns a collection of URL
   hits, and a set of parsed pages."
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


;;;;;;;;;;;;;;;;;;;;
;; DB
;;;;;;;;;;;;;;;;;;;;

(defn new-db-conn []
  (let [schema {:page/url {:db/unique :db.unique/identity}
                :page/links {:db/valueType :db.type/ref
                             :db/cardinality :db.cardinality/many}}]
    (d/create-conn schema)))

(defn parsed-tx
  "Creates transaction for adding parsed pages to DB. Turns :page/links into
   :db.type/ref references to other :page/urls."
  [parsed]
  (update parsed
          :page/links
          (fn [links]
            (map (fn [s] {:page/url s})
                 links))))
