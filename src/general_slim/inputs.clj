(ns general-slim.inputs
  (:require [general-slim.forces :refer [unit-in-square refresh-move-points]]
            [general-slim.utils :refer [dissoc-in]]))

(defn adjacents [[x y]]
  #{[(inc x) y] [(dec x) y]
    [x (inc y)] [x (dec y)]})

(def other-side {:red :blue :blue :red})

(defn move-unit [game-state side unit-id to-square]
  (let [current-pos (get-in game-state [side :units unit-id :position])
        move-points (get-in game-state [side :units unit-id :move-points])]
    (cond
      (nil? ((adjacents current-pos) to-square))
      (do (println "Cannot move to an non-adjacent square" side unit-id current-pos to-square)
          game-state)
      (unit-in-square game-state to-square)
      (do (println "Cannot move to an occupied square")
          game-state)
      (zero? move-points) ;; probably should use can-move
      (do (println "Unit doesn't have enough move points")
          game-state)
      :else (-> game-state
                (assoc-in [side :units unit-id :position] to-square)
                (update-in [side :units unit-id :move-points] dec)))))

'[:attack my-side (:id selected-unit?) cursor]

(defn resolve-combat [unit1 unit2]
  [unit1 (assoc unit2 :hp 0)])

(resolve-combat {:id :inf1, :unit-type :infantry, :hp 10, :position [7 6], :side :red, :move-points 1}
                {:id :inf1, :unit-type :infantry, :hp 10, :position [7 6], :side :blue, :move-points 1})

(defn update-unit [game-state unit]
  (if (> (:hp unit) 0)
    (assoc-in game-state [(:side unit) :units (:id unit)] unit)
    (dissoc-in game-state [(:side unit) :units] (:id unit))))

(defn attack-order [game-state my-side my-unit-id enemy-unit-id]
  (let [my-unit (get-in game-state [my-side :units my-unit-id])
        enemy-unit (get-in game-state [(other-side my-side) :units enemy-unit-id])
        [u1 u2] (resolve-combat my-unit enemy-unit)]
    (-> game-state
        (update-unit u1)
        (update-unit u2))))

(attack-order {:red {:units {:inf1 {:id :inf1, :unit-type :infantry, :hp 10, :position [7 6], :side :red, :move-points 1}}}
               :blue {:units {:inf1 {:id :inf1, :unit-type :infantry, :hp 10, :position [7 6], :side :blue, :move-points 1}}}}
              :red :inf1 :inf1)

(defn end-turn [game-state side]
  (if (= side (:turn game-state))
    (-> game-state
        (assoc :turn (if (= :red side) :blue :red))
        (update-in [side :units] refresh-move-points))
    (throw (ex-info "Cannot end turn for this side, not their turn" {:side side}))))

(defn handle-input [game-state input]
  (let [[order-type side unit target] input]
    (case order-type
      :move (move-unit game-state side unit target)
      :end-turn (end-turn game-state side)
      :attack (do (println "Attacking") (attack-order game-state side unit target)))))

'[:end-turn (:turn game-state)]
'[:attack my-side (:id selected-unit?) (:id unit-under-cursor?)]
'[:move my-side (:id selected-unit?) cursor]