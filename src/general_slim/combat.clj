(ns general-slim.combat
  (:require [general-slim.utils :refer [average rng-int]]))

(defn calculate-hits
  [shots enemy-men]
  (count (reduce (fn [s t]
                   (if (< (rand) 0.20)
                     (conj s t)
                     s))
                 #{}
                 (take shots (rng-int enemy-men)))))

(comment
  (for [x [100 200 300 400 500 600 700 800 900 1000]]
    [:force-proportion (float (/ x 1000))
     :casualty-rate (int (* 100 (/ (average (repeatedly 1000 #(calculate-hits 1000 x))) x)))]))

(defn resolve-combat [assaulting-unit defending-unit]
  (let [a-cas (calculate-hits (:soldiers assaulting-unit)
                              (:soldiers defending-unit))
        d-cas (calculate-hits (:soldiers defending-unit)
                              (:soldiers assaulting-unit))]
    [(update assaulting-unit :soldiers - a-cas)
     (update defending-unit :soldiers - d-cas)]))
