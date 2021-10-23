(ns general-slim.inputs
  (:require [general-slim.forces :refer [unit-in-square]]))

(defn adjacents [[x y]]
  #{[(inc x) y] [(dec x) y]
    [x (inc y)] [x (dec y)]})

(defn move-unit [game-state side unit-id to-square]
  (let [current-pos (get-in game-state [side :units unit-id :position])]
    (println "current pos" current-pos)
    (cond
      (nil? ((adjacents current-pos) to-square))
      (do (println "Cannot move to an non-adjacent square" side unit-id current-pos to-square)
          game-state)
      (unit-in-square game-state to-square)
      (do (println "Cannot move to an occupied square")
          game-state)
      :else (assoc-in game-state [side :units unit-id :position] to-square))))

(defn end-turn [game-state side]
  (if (= side (:turn game-state))
    (assoc game-state :turn (if (= :red side) :blue :red))
    (throw (ex-info "Cannot end turn for this side, not their turn" {:side side}))))

(defn handle-input [game-state input]
  (let [[order-type side unit order] input]
    (case order-type
      :move (move-unit game-state side unit order)
      :end-turn (end-turn game-state side))))
