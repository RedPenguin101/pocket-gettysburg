(ns general-slim.scenario-loader
  "A Scenario resource is a specification of an initial gamestate
   It is stored as edn, and defines various things about
   how to set up the game, like the map, initial units,
   which side starts. For example:
   
   {:map \"aw_ft1\"
    :red [[:infantry \"73rd New York Regiment\" \"73rd\" [5 3]]
          [:infantry \"86rd Illinois Regiment\" \"86th\" [3 5]]
          [:general \"Brigadier General Ironside\" \"X\" [3 3]]]
    :blue [[:infantry \"15th Virginia Regiment\" \"15th\" [13 5]]
           [:infantry \"23rd Virginia Regiment\" \"23rd\" [13 8]]]
    :turn :red
    :turn-number 0}
   
   The units described in the scenario are turned into the
   units that the game understands by reference to
   'unit templates', stored in `units.edn` in resources.

   (this is actually done in the 'forces' namespace with the
   'make-unit' function, but the equivalent function here
   also randomly generates the UUID, which probably it shouldn't)

   The output of the scenario loader is the game-state,
   
   "

  (:require [clojure.edn :as edn]
            [general-slim.map-loader :refer [load-map]]
            [general-slim.forces :refer [make-unit]]
            [general-slim.field :refer [field-size]]))

(defn- mu
  [side unit-templates]
  (fn [[type name short pos]]
    (let [id (str (java.util.UUID/randomUUID))]
      [id (make-unit type side id name short pos unit-templates)])))

(defn- make-units [side units unit-templates]
  {:units (into {} (map (mu side unit-templates) units))})

(defn- add-field-size [game-state]
  (assoc game-state :field-size (field-size (:field game-state))))

(defn load-scenario [scenario-name]
  (let [unit-templates (edn/read-string (slurp (str "resources/units.edn")))
        {:keys [map red blue turn turn-number]}
        (edn/read-string (slurp (str "resources/scenarios/" scenario-name ".edn")))]
    (add-field-size {:field (load-map map)
                     :turn turn :turn-number turn-number
                     :red (make-units :red red unit-templates)
                     :blue (make-units :blue blue unit-templates)})))

(comment
  (dissoc (load-scenario "basic") :field))