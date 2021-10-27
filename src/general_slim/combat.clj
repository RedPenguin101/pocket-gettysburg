(ns general-slim.combat
  (:require [general-slim.utils :refer [average rng-int zero-floored-minus]]))

(def hit-rates
  {:field 0.2
   :trees 0.1
   :mountains 0.2
   :road 0.2})

(defn calculate-hits
  [shots enemy-men hit-rate]
  (count (reduce (fn [s t]
                   (if (< (rand) hit-rate)
                     (conj s t)
                     s))
                 #{}
                 (take shots (rng-int enemy-men)))))

(comment
  (for [x [100 200 300 400 500 600 700 800 900 1000]]
    [:force-proportion (float (/ x 1000))
     :casualty-rate (int (* 100 (/ (average (repeatedly 1000 #(calculate-hits 1000 x))) x)))]))

(defn print-combat-report [volleying-unit other-unit casualties]
  (println (:soldiers volleying-unit) (name (:side volleying-unit))
           "fired on" (:soldiers other-unit) (name (:side other-unit))
           "in" (name (:terrain other-unit))
           "causing" casualties "casualties"))

(defn resolve-combat
  ([assaulting-unit defending-unit] (resolve-combat assaulting-unit defending-unit 5))
  ([a-unit d-unit rounds]
   (if (or (zero? rounds)
           (<= (:soldiers a-unit) 0)
           (<= (:soldiers d-unit) 0))
     [(dissoc a-unit :terrain) (dissoc d-unit :terrain)]
     (let [d-cas (calculate-hits (:soldiers a-unit)
                                 (:soldiers d-unit)
                                 (hit-rates (:terrain d-unit)))
           a-cas (calculate-hits (:soldiers d-unit)
                                 (:soldiers a-unit)
                                 (hit-rates (:terrain a-unit)))]
       (println "Volley" (- 6 rounds) ":")
       (print-combat-report a-unit d-unit d-cas)
       (print-combat-report d-unit a-unit a-cas)
       (recur (update a-unit :soldiers zero-floored-minus a-cas)
              (update d-unit :soldiers zero-floored-minus d-cas)
              (dec rounds))))))
