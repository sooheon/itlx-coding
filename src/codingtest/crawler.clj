(ns codingtest.crawler
  (:require
   [clj-http.client :as client]
   [jsonista.core :as j]
   [cemerick.url :refer [url url-encode]]
   [codingtest.utils :as u]
   [datascript.core :as d]))


;;;;;;;;;;;;;;;;;;;;
;; In memory DB
;;;;;;;;;;;;;;;;;;;;

(def conn (d/create-conn))

(def schema
  {:page/url {}
   :page/links {:db/cardinality :db.cardinality/many} ;; page/hits is # of links backwards
   :response/status {}})

(d/transact conn [schema])


;;;;;;;;;;;;;;;;;;;;
;; Crawler
;;;;;;;;;;;;;;;;;;;;

(def root
  (url "https://tyruiop.org/crawler/"))

(defn form-url [rel-path]
  (str (url root rel-path)))

(def get
  (memoize
   (fn [rel-path]
     (client/get (form-url rel-path) {:throw-exceptions false}))))

(defn parse-body-json
  [response]
  (-> response
      :body
      (j/read-value (j/object-mapper {:decode-key-fn true}))))

(defn parse-page [rel-path]
  (let [url (form-url rel-path)
        resp (get rel-path)]
    (case (:status resp)
      404 {:page/url url :response/status 404}
      200 (let [json (parse-body-json resp)]
            {:page/url url :response/status 200
             :page/links (:links json)
             :page/title (:title json)}))))

