(ns general-slim.viewsheds
  (:require [general-slim.utils :refer [adjacent? remove-oob-coords]]
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

(defn add-viewsheds [game-state]
  (->> (:turn game-state)
       (forces/occupied-grids game-state)
       (mapcat #(calculate-viewsheds game-state %))
       (remove-oob-coords (first (:field-size game-state)) (second (:field-size game-state)))
       (set)
       (assoc game-state :viewsheds)))
