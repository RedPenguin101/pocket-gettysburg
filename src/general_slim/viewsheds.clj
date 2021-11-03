(ns general-slim.viewsheds
  (:require [clojure.set :as set]
            [general-slim.utils :refer [adjacent? remove-oob-coords]]
            [general-slim.forces :as forces]
            [general-slim.field :as field]
            [general-slim.bresenham :as br]))

(defn paths [n loc]
  (let [edges (set (br/bresenham-circle loc n))]
    (for [point edges]
      (rest (br/bresenham-line loc point)))))

(defn walk-path [path loc tile-terrain]
  (reduce (fn [out-path next-tile]
            (case (get tile-terrain next-tile)
              :trees (reduced (if (and (adjacent? loc next-tile) (empty? out-path)) [next-tile] out-path))
              :mountains (reduced (conj out-path next-tile))
              (conj out-path next-tile)))
          []
          path))

(defn viewshed [loc my-terrain tmap]
  (conj (set (mapcat #(walk-path % loc tmap) (if (= :mountains my-terrain) (paths 5 loc) (paths 4 loc)))) loc))

(defn calculate-viewsheds [game-state unit-loc]
  (viewshed unit-loc (field/terrain-at (:field game-state) unit-loc) (field/terrain-map (:field game-state))))

(defn add-viewshed-to-units [game-state]
  (let [side (:turn game-state)
        units (vals (get-in game-state [side :units]))]
    (reduce (fn [gs unit]
              (assoc-in gs [side :units (:id unit) :viewshed]
                        (calculate-viewsheds game-state (:position unit))))
            game-state
            units)))

(defn all-viewsheds [game-state]
  (apply set/union (map :viewshed (forces/units game-state (:turn game-state)))))

(defn add-combined-viewsheds [game-state]
  (assoc game-state :viewsheds (all-viewsheds game-state)))

(defn add-viewsheds [game-state]
  (add-combined-viewsheds (add-viewshed-to-units game-state)))
