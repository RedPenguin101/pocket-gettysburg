(ns general-slim.viewsheds
  (:require [general-slim.utils :refer [adjacent? coord+ remove-oob-coords]]
            [general-slim.forces :as forces]
            [general-slim.field :as field]))

(defn paths [loc]
  [(mapv #(coord+ loc %) [[0 -1] [0 -2] [0 -3] [0 -4]])
   (mapv #(coord+ loc %) [[1 -1] [1 -2] [1 -3]])
   (mapv #(coord+ loc %) [[1 -1] [2 -2]])
   (mapv #(coord+ loc %) [[1 -1] [2 -1] [3 -1]])
   (mapv #(coord+ loc %) [[1 0] [2 0] [3 0] [4 0]])
   (mapv #(coord+ loc %) [[1 1] [2 1] [3 1]])
   (mapv #(coord+ loc %) [[1 1] [2 2]])
   (mapv #(coord+ loc %) [[1 1] [1 2] [1 3]])
   (mapv #(coord+ loc %) [[0 1] [0 2] [0 3] [0 4]])
   (mapv #(coord+ loc %) [[-1 1] [-1 2] [-1 3]])
   (mapv #(coord+ loc %) [[-1 1] [-2 2]])
   (mapv #(coord+ loc %) [[-1 1] [-2 1] [-3 1]])
   (mapv #(coord+ loc %) [[-1 0] [-2 0] [-3 0] [-4 0]])
   (mapv #(coord+ loc %) [[-1 -1] [-2 -1] [-3 -1]])
   (mapv #(coord+ loc %) [[-1 -1] [-2 -2]])
   (mapv #(coord+ loc %) [[-1 -1] [-1 -2] [-1 -3]])])

(defn walk-path [path loc tile-terrain]
  (reduce (fn [out-path next-tile]
            (case (get tile-terrain next-tile)
              :trees (reduced (if (and (adjacent? loc next-tile) (empty? out-path)) [next-tile] out-path))
              :mountains (reduced (conj out-path next-tile))
              (conj out-path next-tile)))
          []
          path))

(defn viewshed [loc tmap]
  (set (mapcat #(walk-path % loc tmap) (paths loc))))

(defn calculate-viewsheds [game-state unit-loc]
  (viewshed unit-loc (field/terrain-map (:field game-state))))

(defn add-viewsheds [game-state]
  (->> (:turn game-state)
       (forces/occupied-grids game-state)
       (mapcat #(calculate-viewsheds game-state %))
       (remove-oob-coords (first (:field-size game-state)) (second (:field-size game-state)))
       (set)
       (assoc game-state :viewsheds)))
