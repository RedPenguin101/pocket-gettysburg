(ns general-slim.forces
  (:require [general-slim.utils :refer [map-vals in-view?]]))

(defn make-unit [type side id pos unit-templates]
  (assoc (type unit-templates)
         :id id :side side :position pos))

(defn units
  "Returns a sequence of every unit
   The two arity version returns only units of one side"
  ;; I think the assoc side is redundent now
  ([game-state]
   (concat (map #(assoc % :side :red) (vals (get-in game-state [:red :units])))
           (map #(assoc % :side :blue) (vals (get-in game-state [:blue :units])))))
  ([game-state side]
   (map #(assoc % :side side) (vals (get-in game-state [side :units])))))

(defn occupied-grids
  ([game-state] (set (map :position (units game-state))))
  ([game-state side] (set (map :position (units game-state side)))))

(defn unit-in-square
  "Returns the unit occupying the square, or nil if none"
  [game-state square]
  (first (filter #(= square (:position %)) (units game-state))))

(defn can-move? [unit]
  (> (:move-points unit) 0))

(defn refresh-units [unit-map]
  (->> unit-map
       (map-vals #(assoc % :move-points (:max-move-points %) :can-attack true))))

(defn defence-value [unit terrain]
  (get-in unit [:terrain-defense terrain]))

(defn unit-in-view? [unit camera]
  (in-view? camera (:position unit)))