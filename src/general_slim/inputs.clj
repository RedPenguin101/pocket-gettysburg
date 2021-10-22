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
      (throw (ex-info "Cannot move to an non-adjacent square" {:current-square current-pos
                                                               :target-square to-square}))
      (unit-in-square game-state to-square)
      (throw (ex-info "Cannot move to an occupied square" {:square to-square}))
      :else (assoc-in game-state [side :units unit-id :position] to-square))))

(defn handle-input [game-state input]
  (let [[side unit order-type order] input]
    (case order-type
      :move (move-unit game-state side unit order))))