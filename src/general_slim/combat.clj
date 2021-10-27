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
     :casualty-rate (int (* 100 (/ (average (repeatedly 1000 #(calculate-hits 1000 x 0.2))) x)))]))

(defn print-combat-report [volleying-unit other-unit casualties]
  (println (:soldiers volleying-unit) (name (:side volleying-unit))
           "fired on" (:soldiers other-unit) (name (:side other-unit))
           "in" (name (:terrain other-unit))
           "causing" casualties "casualties"))

(defn prep-unit
  "A few things get added to a unit structure before they 
   start combat to help with calculation"
  [unit]
  (-> unit
      (assoc :starting-strength (:soldiers unit))))

(defn clean-unit
  "Combat adds some gumf to a unit. This function cleans that
   out before returning the unit to the main game loop"
  [unit]
  (dissoc unit :terrain :starting-strength :last-round-casualties))

(defn incur-casualties [receiving shooting]
  (let [cas (calculate-hits (:soldiers shooting)
                            (:soldiers receiving)
                            (hit-rates (:terrain receiving)))]
    (print-combat-report shooting receiving cas)
    (-> receiving
        (update :soldiers zero-floored-minus cas)
        (assoc :last-round-casualties cas))))

(defn resolve-combat
  ([assaulting-unit defending-unit] (resolve-combat (prep-unit assaulting-unit) (prep-unit defending-unit) 5))
  ([a-unit d-unit rounds]
   (println "Volley" (- 6 rounds) ":")
   (if (some zero? [rounds (:soldiers a-unit) (:soldiers d-unit)])
     [(clean-unit a-unit) (clean-unit d-unit)]
     (recur (incur-casualties a-unit d-unit)
            (incur-casualties d-unit a-unit)
            (dec rounds)))))
