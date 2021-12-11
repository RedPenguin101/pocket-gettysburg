(ns general-slim.simulation.moving)

"This NS contains everything necessary to move units
 from one point to another. This includes how a unit
 will calculate its route from point A to point B"

(defn change-position [unit new-position]
  (assoc unit :position new-position))


(defn plan-route
  "When part of a unit's objective is to move to a new location,
   it comes up with a route to reach that location. This will
   factor in the terrain around it, for example it will use
   roads to reach it's destination in the shortest time."
  [unit new-position terrain])

;; Example units

{"70bfe61e-356c-49d0-9ee6-9828e50a4d13"
 {:short-name "86th"
  :move-points 3
  :max-move-points 3
  :unit-type :infantry
  :soldiers 1000
  :movement-table {:field 1, :road 0.5, :trees 1, :mountains 2}
  :move-over false
  :viewshed
  #{[7 6] [4 3] [2 2] [3 9] [7 7] [2 8] [2 3] [2 5] [6 7] [7 4] [3 3] [5 4] [1 1] [6 3] [0 5] [3 4] [7 3] [4 2] [-1 3]}
  :id "70bfe61e-356c-49d0-9ee6-9828e50a4d13"
  :side :red
  :unit-name "86rd Illinois Regiment"
  :position [3 5]}}