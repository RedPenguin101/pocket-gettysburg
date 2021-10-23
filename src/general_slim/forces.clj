(ns general-slim.forces)

(def red {:units {:inf1 {:id :inf1 :unit-type :infantry
                         :position [2 2] :side :red}
                  :inf2 {:id :inf2 :unit-type :infantry
                         :position [3 3] :side :red}}})

(def blue {:units {:inf1 {:id :inf1 :unit-type :infantry
                          :position [7 7] :side :blue}
                   :inf2 {:id :inf2 :unit-type :infantry
                          :position [8 8] :side :blue}}})

(defn units [game-state]
  (concat (map #(assoc % :side :red) (vals (get-in game-state [:red :units])))
          (map #(assoc % :side :blue) (vals (get-in game-state [:blue :units])))))

(defn unit-in-square
  "Returns the unit occupying the square, or nil if none"
  [game-state square]
  (first (filter #(= square (:position %)) (units game-state))))
