(ns general-slim.combat-test
  (:require [clojure.test :refer [deftest is are]]
            [general-slim.combat :as SUT]))

(deftest trivial-cases
  (is (SUT/resolve-combat! {:soldiers 100 :terrain :field}
                           {:soldiers 100 :terrain :field}))
  (are [unit] (thrown? Exception (SUT/resolve-combat! unit unit))
    {}
    {:soldiers 100}
    {:soldiers 100 :terrain :fake-terrain}
    {:soldiers 0 :terrain :field}
    {:soldiers -5 :terrain :field}))

(deftest calc-hits
  (are [kills solution] (= kills (SUT/calculate-hits 0.2 solution))
    ;; zero is perfect accuracy
    3 [[0 0] [0 1] [0 2]]
    ;; 1 is wild shot
    0 [[1 0] [1 1] [1 2]]
    ;; any shots with accuracy below the hit-rate are hits
    1 [[0.1 0] [1 1] [1 2]]
    ;; if everyone fires at the same guy, even if they're accurate, 
    ;; only that guy dies
    1 [[0 1] [0 1] [0 1]]))

(comment
  (SUT/resolve-combat! {:soldiers 100 :terrain :field}
                       {:soldiers 100 :terrain :field}))
