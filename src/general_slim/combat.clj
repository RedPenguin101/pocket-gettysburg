(ns general-slim.combat
  (:require [general-slim.utils :as u]
            [general-slim.random :as rng]))

(def hit-rates
  {:field 0.2
   :trees 0.1
   :mountains 0.2
   :road 0.2})

(defn calculate-hits
  "Calculates the number of men hit the enemy will suffer, given
   the number of guns firing at them, and the hit rate of those guns.

   The hit-solution that needs to be passed in is a sequence of tuples
   as long as the number of guns firing, and consist of tuple of:
   [shot-accurancy enemy-fired-at]
   1. a float between 0 and 1. 0 is dead on, 1 is wildly inaccurate
   2. an integer between zero and the number of enemy men."
  [hit-rate fire-solution]
  (->> fire-solution
       (remove #(< hit-rate (first %)))
       (map second)
       (set)
       (count)))

(defn- prep-unit
  "A few things get added to a unit structure before they 
   start combat to help with calculation"
  [unit]
  (-> unit
      (assoc :starting-strength (:soldiers unit))))

(defn- clean-unit
  "Combat adds some gumf to a unit. This function cleans that
   out before returning the unit to the main game loop"
  [unit]
  (dissoc unit :terrain :starting-strength :last-round-casualties))

(defn- retreat-threshold [unit opposing-unit]
  (let [prop-strength (/ (:soldiers unit) (:soldiers opposing-unit))
        prop-casualties (- 1 (/ (:soldiers unit) (:starting-strength unit)))]
    (* (+ (Math/pow prop-casualties 1.5)
          (- 1 prop-strength))
       (if (= :trees (:terrain opposing-unit)) 1.2 1)
       (if (= :trees (:terrain unit)) 0.8 1))))

(defn- retreater
  "Given two units, will return :attacker if the first unit
   retreats, :defender if the second, nil if neither.
   In the case that both units want to retreat, the retreating
   unit depends on the boolean value of a-retreats-first"
  [a-unit d-unit a-retreats-first roll]
  (let [a-retreat? (< roll (retreat-threshold a-unit d-unit))
        d-retreat? (< roll (retreat-threshold d-unit a-unit))]
    (cond (and a-retreat? d-retreat?)
          (if a-retreats-first :attacker-retreats :defender-retreats)
          a-retreat? :attacker-retreats
          d-retreat? :defender-retreats
          :else nil)))

(defn- incur-casualties [receiving-unit hit-solution]
  (let [cas (calculate-hits (hit-rates (:terrain receiving-unit)) hit-solution)]
    (-> receiving-unit
        (update :soldiers u/zero-floored-minus cas)
        (assoc :last-round-casualties cas))))

(defn- valid-unit? [unit]
  (let [required-fields #{:soldiers :terrain}]
    (and (every? #(contains? unit %) required-fields)
         ((set (keys hit-rates)) (:terrain unit))
         (pos-int? (:soldiers unit)))))

(defn- can-resolve? [unit1 unit2]
  (every? valid-unit? [unit1 unit2]))

(defn resolve-round [a-unit d-unit a-retreats-first? retreat-roll a-hit-solution d-hit-solution]
  (let [retreater? (retreater a-unit d-unit a-retreats-first? retreat-roll)]
    (cond (some zero? [(:soldiers a-unit) (:soldiers d-unit)])
          [:turn-finished a-unit d-unit]

          retreater?
          (case retreater?
            :attacker-retreats [retreater? (incur-casualties a-unit d-hit-solution) d-unit]
            :defender-retreats [retreater?  a-unit (incur-casualties d-unit a-hit-solution)])

          :else [:round-finished
                 (incur-casualties a-unit d-hit-solution)
                 (incur-casualties d-unit a-hit-solution)])))

;; Below here non-deterministic functions

(defn- random-fire-solution! [guns enemy]
  (map vector
       (repeatedly rand)
       (take guns (rng/rng-int! enemy))))

(defn resolve-combat!
  "Resolve combat takes 2 units, and returns a tuple of
   [resolution unit unit], where the units are the state of the unit
   after combat has been resolved.

   Resolution can be turn-finished, attacker-retreats or defender-retreats
   
   It assumes that the terrain each unit is standing on has been 
   added to the unit as a value. The terrain is stripped out before 
   the units are returned to the caller"
  ([assaulting-unit defending-unit]
   (if (can-resolve? assaulting-unit defending-unit)
     (resolve-combat! (prep-unit assaulting-unit) (prep-unit defending-unit)
                      (rng/rand-bool!) (rand)
                      5)
     (throw (ex-info "Can't resolve with these units" {:attacker assaulting-unit
                                                       :defender defending-unit}))))
  ([a-unit d-unit a-retreats-first retreat-roll rounds]
   (let [[round-resolution new-a new-d]
         (resolve-round a-unit d-unit a-retreats-first retreat-roll
                        (random-fire-solution! (:soldiers a-unit) (:soldiers d-unit))
                        (random-fire-solution! (:soldiers d-unit) (:soldiers a-unit)))]
     (cond (zero? rounds)
           [:turn-finished (clean-unit a-unit) (clean-unit d-unit)]

           (= :round-finished round-resolution)
           (recur new-a
                  new-d
                  a-retreats-first retreat-roll
                  (dec rounds))

           :else [round-resolution (clean-unit new-a) (clean-unit new-d)]))))
