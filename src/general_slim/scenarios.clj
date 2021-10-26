(ns general-slim.scenarios
  (:require [clojure.edn :as edn]
            [general-slim.map-loader :refer [load-map]]
            [general-slim.forces :refer [make-unit]]
            [general-slim.field :refer [field-size]]))

(defn mu [side unit-templates]
  (fn [[type name pos]]
    [name (make-unit type side name pos unit-templates)]))

(defn make-units [side units unit-templates]
  {:units (into {} (map (mu side unit-templates) units))})

(defn add-field-size [game-state]
  (assoc game-state :field-size (field-size (:field game-state))))

(defn load-scenario [scenario-name]
  (let [unit-templates (edn/read-string (slurp (str "resources/units.edn")))
        {:keys [map red blue turn turn-number]}
        (edn/read-string (slurp (str "resources/scenarios/" scenario-name ".edn")))]
    (add-field-size {:field (load-map map)
                     :turn turn :turn-number turn-number
                     :red (make-units :red red unit-templates)
                     :blue (make-units :blue blue unit-templates)})))

(dissoc (load-scenario "basic") :field)