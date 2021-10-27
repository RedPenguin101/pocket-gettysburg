(ns general-slim.combat
  (:require [general-slim.utils :refer [average rng-int zero-floored-minus]]))

(def noisy-print true)

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

(defn retreat-threshold [unit opposing-unit]
  (let [prop-strength (/ (:soldiers unit) (:soldiers opposing-unit))
        prop-casualties (- 1 (/ (:soldiers unit) (:starting-strength unit)))]
    #_(println (:side unit) "PropCas" (Math/pow prop-casualties 1.5) "PropStr" (float (- 1 prop-strength)))
    (* (+ (Math/pow prop-casualties 1.5)
          (- 1 prop-strength))
       (if (= :trees (:terrain opposing-unit)) 1.2 1)
       (if (= :trees (:terrain unit)) 0.8 1))))

(defn retreater
  "Given two units, will return :attacker if the first unit
   retreats, :defender if the second, nil if neither"
  [a-unit d-unit]
  (let [a-retreat? (< (rand) (retreat-threshold a-unit d-unit))
        d-retreat? (< (rand) (retreat-threshold d-unit a-unit))]
    (cond (and a-retreat? d-retreat?)
          ;; if both, randomly pick one
          (if (zero? (rand-int 2)) :attacker-retreats :defender-retreats)
          a-retreat? :attacker-retreats
          d-retreat? :defender-retreats
          :else nil)))

(defn print-combat-report [volleying-unit other-unit casualties]
  (println
   (:soldiers volleying-unit) (name (:side volleying-unit))
   "fired on" (:soldiers other-unit) (name (:side other-unit))
   "in" (name (:terrain other-unit))
   "causing" casualties "casualties"))

(defn print-retreat-chance [unit1 unit2]
  (println "Retreat threshold of" (name (:side unit1)) "is" (retreat-threshold unit1 unit2)))

(defn incur-casualties [receiving shooting]
  (let [cas (calculate-hits (:soldiers shooting)
                            (:soldiers receiving)
                            (hit-rates (:terrain receiving)))]
    (when noisy-print (print-combat-report shooting receiving cas))
    (-> receiving
        (update :soldiers zero-floored-minus cas)
        (assoc :last-round-casualties cas))))

(defn resolve-combat
  ([assaulting-unit defending-unit] (resolve-combat (prep-unit assaulting-unit) (prep-unit defending-unit) 5))
  ([a-unit d-unit rounds]
   (when noisy-print
     (println "Volley" (- 6 rounds) ":")
     (print-retreat-chance a-unit d-unit)
     (print-retreat-chance d-unit a-unit))
   (let [retreater? (retreater a-unit d-unit)]
     (cond (some zero? [rounds (:soldiers a-unit) (:soldiers d-unit)])
           [:round-finished (clean-unit a-unit) (clean-unit d-unit)]

           retreater?
           (do (when noisy-print (println (name retreater?)))
               (case retreater?
                 :attacker-retreats [retreater? (clean-unit (incur-casualties a-unit d-unit)) (clean-unit d-unit)]
                 :defender-retreats [retreater? (clean-unit a-unit) (clean-unit (incur-casualties d-unit a-unit))]))

           :else (recur (incur-casualties a-unit d-unit)
                        (incur-casualties d-unit a-unit)
                        (dec rounds))))))

(comment
  (first (let [red {:can-attack true
                    :unit-type :infantry
                    :soldiers 1000
                    :terrain :field
                    :id :inf1
                    :side :red
                    :position [7 5]}
               blue {:can-attack true
                     :unit-type :infantry
                     :soldiers 500
                     :terrain :field
                     :id :inf1
                     :side :blue
                     :position [7 6]}]
           (resolve-combat red blue)))
  (frequencies (repeatedly 1000 #(first (let [red {:can-attack true
                                                   :unit-type :infantry
                                                   :soldiers 1000
                                                   :terrain :field
                                                   :id :inf1
                                                   :side :red
                                                   :position [7 5]}
                                              blue {:can-attack true
                                                    :unit-type :infantry
                                                    :soldiers 900
                                                    :terrain :field
                                                    :id :inf1
                                                    :side :blue
                                                    :position [7 6]}]
                                          (resolve-combat red blue))))))