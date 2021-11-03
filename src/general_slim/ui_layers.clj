(ns general-slim.ui-layers
  (:require [general-slim.field :as field]
            [clojure.set :as set]
            [general-slim.game :as game]
            [general-slim.forces :as forces]))

(def other-side {:red :blue :blue :red})

(defn constants [game-state]
  (select-keys game-state [:images :camera]))

(defn field-layer [game-state]
  (vals (field/terrain-in-view (:field game-state) (:camera game-state))))

(defn route-layer [game-state]
  (:route game-state))

(defn highlight-layer [game-state]
  (:highlight game-state))

(defn unit-layer [game-state]
  (let [my-side (:turn game-state)
        my-units (forces/units game-state my-side)
        viewsheds (:viewsheds game-state)
        visible-enemy-units (filter #(viewsheds (:position %)) (forces/units game-state (other-side my-side)))]
    (concat my-units visible-enemy-units)))

(defn viewsheds-layer [game-state]
  (set/difference (set (keys (:field game-state))) (:viewsheds game-state)))

(defn cursor-layer [game-state]
  {:type (if (:attack-mode game-state) :attack :move)
   :cursor (:cursor game-state)})

(defn hover-info-layer [game-state]
  (when (and (:unit-under-cursor game-state)
             ((:viewsheds game-state) (:cursor game-state)))
    (:unit-under-cursor game-state)))

(defn menu-layer [game-state]
  (:menu game-state))

(defn debug-layer [game-state]
  (when (:debug game-state) (game/debug-data game-state)))

(defn turn-indicator-layer [game-state]
  [(:turn game-state) (inc (int (/ (:turn-number game-state) 2)))])
