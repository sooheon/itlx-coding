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
         (parse-json (:body (GET "index.json"))))))

(deftest json-parse-test
  (let [broken-json "{
      \"title\": \"Data 3\",
      \"links\": [
          \"data1.json\",
          \"data2.json\",
          \"data6.json\",,,
      ]
  }
  "]
    (is (= {:title "Data 3"
            :links ["data1.json"
                    "data2.json"
                    "data6.json"]}
           (parse-json broken-json)))))

(deftest visit-all-pages!-test
  (is (= 10 (count (:parsed (visit-all-pages! "index.json"))))))
