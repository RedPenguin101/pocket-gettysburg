(ns general-slim.inputs-test
  (:require [clojure.test :refer [deftest is]]
            [general-slim.field :refer [flat-field]]
            [general-slim.forces :refer [make-unit unit-at-location]]
            [general-slim.inputs :as SUT]))

(def trees {:field (-> (flat-field 10 10)
                       (assoc [4 6] {:grid [4 6] :terrain :trees})
                       (assoc [5 4] {:grid [5 4] :terrain :trees})
                       (assoc [5 9] {:grid [5 9] :terrain :trees}))
            :red {:units {:cav1 (make-unit :cavalry :red :cav1 [5 6])}}
            :blue {:units {}}
            :turn :red
            :turn-number 0
            :cursor [5 5]})


(deftest square-accessibility
  (is (= #{[7 6] [7 7] [6 7] [8 6] [6 6] [4 7] [6 5] [4 6] [5 7] [4 8] [6 4] [5 6] [5 8] [6 8] [5 5] [4 5] [4 4] [3 7] [7 5] [3 5]}
         (SUT/can-move-to trees (unit-at-location trees [5 6])))))

