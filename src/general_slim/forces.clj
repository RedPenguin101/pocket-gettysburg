(ns general-slim.forces
  (:require [clojure.spec.alpha :as spec]))

(defn make-unit [type side id name short pos unit-templates]
  (assoc (type unit-templates)
         :id id :side side :position pos :unit-name name :short-name short))

;; Operations on a single unit
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset [unit]
  (assoc unit
         :move-points (:max-move-points unit)
         :move-over false))

(defn defence-value [unit terrain]
  (get-in unit [:terrain-defense terrain]))

(defn can-move? [unit]
  (> (:move-points unit) 0))

(defn move [unit location]
  (assoc unit :position location))

;; Operations on a map of id->units
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn refresh-units [units]
  (update-vals units reset))

;; Various ways to get units from a game-state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn units
  "Returns a sequence (not a map) of the units for the given
   side."
  ([game-state]
   (concat (units game-state :red) (units game-state :blue)))
  ([game-state side]
   (vals (get-in game-state [side :units]))))

(defn units-by-location
  "Returns a map of coord->unit"
  ([game-state]
   (->> game-state
        units
        (map (fn [unit] [(:position unit) unit]))
        (into {})))
  ([game-state side]
   (->> (units game-state side)
        (map (fn [unit] [(:position unit) unit]))
        (into {}))))

(defn unit-at-location [game-state location]
  (->> game-state
       units-by-location
       (filter (fn [[k _v]] (#{location} k)))
       (into {})
       first
       second))

(defn units-at-locations [game-state locations]
  (select-keys (units-by-location game-state) (vec locations)))

(spec/fdef units-at-locations
  :args (spec/cat
         :game-state map?
         :locations (spec/nilable
                     (spec/coll-of :general-slim.specs/coord)))
  :ret map?)

(defn occupied-grids
  ([game-state side]
   (set (keys (units-by-location game-state side))))
  ([game-state]
   (set (keys (units-by-location game-state)))))

(defn unit-with-id [game-state unit-id]
  (or (get-in game-state [:red :units unit-id])
      (get-in game-state [:blue :units unit-id])))
