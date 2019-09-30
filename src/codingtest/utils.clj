(ns codingtest.utils
  (:require [clojure.core.async :as a]
            [clojure.string :as str]))


(defn parallelize
  "Given a `fnc` capable of creating a transducer when called with 1-arity
   such as `map`, `filter`, or `mapcat`, returns a fn which runs given `fnc` in
   parallel."
  [fnc]
  (fn parallel-fn
    ([concurrent f xs]
     (let [output-chan (a/chan)]
       (a/pipeline-blocking concurrent
                            output-chan
                            (fnc f)
                            (a/to-chan xs))
       (a/<!! (a/into [] output-chan))))
    ([f xs] (parallel-fn (.availableProcessors (Runtime/getRuntime)) f xs))))

(def pmap! (parallelize map))

(def pfilter! (parallelize filter))

(def pmapcat! (parallelize mapcat))

(defn json-remove-trailing-comma
  "See: https://stackoverflow.com/questions/34344328/json-remove-trailiing-comma-from-last-object"
  [s]
  (str/replace s #"\,(?!\s*?[\{\[\"\'\w])" ""))
