(ns codingtest.crawler-test
  (:require
   [clojure.test :refer :all]
   [codingtest.crawler :refer :all]
   [datascript.core :as d]))

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
         (parse-json (:body (GET (make-url "index.json")))))))

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

(deftest parse-page-test
  (is (= (parse-page (make-url "index.json"))
         {:page/url "https://tyruiop.org/crawler/index.json",
          :page/title "Index",
          :response/status 200,
          :page/links '("https://tyruiop.org/crawler/data1.json"
                        "https://tyruiop.org/crawler/data2.json"
                        "https://tyruiop.org/crawler/404.json"
                        "https://tyruiop.org/crawler/data3.json"
                        "https://tyruiop.org/crawler/index.json"
                        "https://tyruiop.org/crawler/data4.json"
                        "https://tyruiop.org/crawler/data5.json"
                        "https://tyruiop.org/crawler/data6.json"
                        "https://tyruiop.org/crawler/data7.json")})))


(deftest visit-all-pages!-test
  (is (= 10 (count (:parsed (visit-all-pages! (make-url "index.json")))))))


(deftest find-hits-test
  (let [conn (new-db-conn)
        URL (make-url "index.json")
        pgs (:parsed (visit-all-pages! URL))]
    (d/transact! conn (map parsed-tx pgs))

    (testing "Counting linked hits manually"
      (is (= (get (count-hits pgs) URL)
             3)))

    (testing "Counting linked hits from datascript"
      (is (= (->> (d/pull (d/db conn) '[:page/_links]
                          [:page/url URL])
                  :page/_links
                  count)
             3)))))

(comment
 (def pgs (:parsed (visit-all-pages! "index.json")))
 (count pgs)

 (d/q '[:find [(pull ?p [:page/url :page/title :response/status {:page/_links [:page/url]}])
               ...]
        :where [?p :page/url]]
      (d/db conn))

 (d/transact conn (map parsed-tx pgs))
 (d/pull (d/db conn) '[*] [:page/url "https://tyruiop.org/crawler/data1.json"])
 (d/q '[:find (pull ?e [*])
        :where [?e :page/url]]
      (d/db new-db-conn))
 (count-hits pgs))
