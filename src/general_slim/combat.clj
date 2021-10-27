(ns general-slim.combat
  (:require [general-slim.utils :refer [average rng-int zero-floored-minus]]))

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

(defn resolve-combat
  ([assaulting-unit defending-unit]
   (resolve-combat assaulting-unit defending-unit 5))
  ([assaulting-unit defending-unit rounds]
   (if (or (zero? rounds)
           (<= (:soldiers assaulting-unit) 0)
           (<= (:soldiers defending-unit) 0))
     [assaulting-unit defending-unit]
     (let [d-cas (calculate-hits (:soldiers assaulting-unit)
                                 (:soldiers defending-unit))
           a-cas (calculate-hits (:soldiers defending-unit)
                                 (:soldiers assaulting-unit))]
       (println "Volley" (- 6 rounds) ":")
       (println (:soldiers assaulting-unit) (name (:side assaulting-unit))
                "fired on" (:soldiers defending-unit) (name (:side defending-unit)) "causing" d-cas "casualties")
       (println (:soldiers defending-unit) (name (:side defending-unit))
                "fired on" (:soldiers assaulting-unit) (name (:side assaulting-unit)) "causing" a-cas "casualties")
       (recur (update assaulting-unit :soldiers zero-floored-minus a-cas)
              (update defending-unit :soldiers zero-floored-minus d-cas)
              (dec rounds))))))
