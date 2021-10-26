(ns general-slim.scenarios
  (:require [clojure.edn :as edn]
            [general-slim.map-loader :refer [load-map]]
            [general-slim.forces :refer [make-unit]]
            [general-slim.field :refer [field-size]]))

(defn mu [side]
  (fn [[type name pos]]
    [name (make-unit type side name pos)]))

(defn make-units [side units]
  {:units (into {} (map (mu side) units))})

(defn add-field-size [game-state]
  (assoc game-state :field-size (field-size (:field game-state))))

(defn load-scenario [scenario-name]
  (let [{:keys [map red blue turn turn-number]}
        (edn/read-string (slurp (str "resources/scenarios/" scenario-name ".edn")))]
    (add-field-size {:field (load-map map)
                     :turn turn :turn-number turn-number
                     :red (make-units :red red)
                     :blue (make-units :blue blue)})))

(dissoc (load-scenario "basic") :field)