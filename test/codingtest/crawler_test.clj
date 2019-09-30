(ns codingtest.crawler-test
  (:require
   [clojure.test :refer :all]
   [codingtest.crawler :refer :all]
   [clj-http.client :as client]))

(deftest crawl-page-test
  (is (= {:title "Index",
          :links ["data1.json"
                  "data2.json"
                  "404.json"
                  "data3.json"
                  "index.json"
                  "data4.json"
                  "data5.json"
                  "data6.json"
                  "data7.json"]}
         (parse-body-json (get "index.json")))))
