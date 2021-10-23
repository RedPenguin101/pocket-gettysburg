(ns general-slim.forces
  (:require [general-slim.utils :refer [map-vals]]))

(def red {:units {:inf1 {:id :inf1 :unit-type :infantry
                         :position [2 2] :side :red :move-points 1}
                  :inf2 {:id :inf2 :unit-type :infantry
                         :position [3 3] :side :red :move-points 1}}})

(def blue {:units {:inf1 {:id :inf1 :unit-type :infantry
                          :position [7 7] :side :blue :move-points 1}
                   :inf2 {:id :inf2 :unit-type :infantry
                          :position [8 8] :side :blue :move-points 1}}})

(defn units
  "Returns a sequence of every unit"
  [game-state]
  (concat (map #(assoc % :side :red) (vals (get-in game-state [:red :units])))
          (map #(assoc % :side :blue) (vals (get-in game-state [:blue :units])))))

(defn unit-in-square
  "Returns the unit occupying the square, or nil if none"
  [game-state square]
  (first (filter #(= square (:position %)) (units game-state))))

(defn can-move? [unit]
  (> (:move-points unit) 0))

(defn refresh-move-points [unit-map]
  (map-vals #(assoc % :move-points 1) unit-map))

(comment
  (refresh-move-points (:units red)))
