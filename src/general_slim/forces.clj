(ns general-slim.forces)

(def side1 {:units {:inf1 {:id :inf1 :unit-type :infantry
                           :position [2 2]}
                    :inf2 {:id :inf2 :unit-type :infantry
                           :position [3 3]}}})

(def side2 {:units {:inf1 {:id :inf1 :unit-type :infantry
                           :position [7 7]}
                    :inf2 {:id :inf2 :unit-type :infantry
                           :position [8 8]}}})

(defn units [game-state]
  (concat (map #(assoc % :side :red) (vals (get-in game-state [:red :units])))
          (map #(assoc % :side :blue) (vals (get-in game-state [:blue :units])))))

(units general-slim.main/game-state)

(defn unit-in-square
  "Returns the unit occupying the square, or nil if none"
  [game-state square]
  (first (filter #(= square (:position %)) (units game-state))))

(unit-in-square general-slim.main/game-state [2 2])