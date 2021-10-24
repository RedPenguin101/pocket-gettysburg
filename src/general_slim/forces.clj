(ns general-slim.forces
  (:require [general-slim.utils :refer [map-vals]]))

(def unit-templates
  {:infantry {:unit-type :infantry :hp 10
              :move-points 2 :max-move-points 2
              :movement-table {:field 1
                               :road 0.5
                               :trees 1
                               :mountains 2}
              :can-attack true
              :attack 5
              :defence {:base 3
                        :modifiers {:road 0
                                    :field 1
                                    :trees 3
                                    :mountains 5}}}
   :cavalry {:unit-type :cavalry :hp 5
             :move-points 3 :max-move-points 3
             :movement-table {:road 0.5
                              :field 1
                              :trees 3
                              :mountains 4}
             :can-attack true
             :attack 3
             :defence {:base 4
                       :modifiers {:road 0
                                   :field 1
                                   :trees 3}}}})

(defn make-unit [type side id pos]
  (assoc (type unit-templates)
         :id id :side side :position pos))

(def red {:units {:inf1 (make-unit :infantry :red :inf1 [2 2])
                  :inf2 (make-unit :infantry :red :inf2 [3 3])}})

(def blue {:units {:inf1 (make-unit :infantry :blue :inf1 [7 7])
                   :inf2 (make-unit :infantry :blue :inf2 [8 8])}})

(defn units
  "Returns a sequence of every unit"
  [game-state]
  (concat (map #(assoc % :side :red) (vals (get-in game-state [:red :units])))
          (map #(assoc % :side :blue) (vals (get-in game-state [:blue :units])))))

(defn unit-in-square
  "Returns the unit occupying the square, or nil if none"
  [game-state square]
  (first (filter #(= square (:position %)) (units game-state))))

(defn can-move? [unit]
  (and (:can-attack unit) (> (:move-points unit) 0)))

(defn refresh-units [unit-map]
  (->> unit-map
       (map-vals #(assoc % :move-points (:max-move-points %) :can-attack true))))

(defn defence-value [unit terrain]
  (+ (get-in unit [:defence :base])
     (get-in unit [:defence :modifiers terrain])))

(comment
  (refresh-units (:units red)))
