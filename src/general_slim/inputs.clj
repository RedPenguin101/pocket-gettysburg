(ns general-slim.inputs
  (:require [general-slim.forces :as forces :refer [can-move? unit-in-square refresh-units]]
            [general-slim.utils :refer [dissoc-in map-vals]]
            [general-slim.field :as field]
            [general-slim.route-calc :as routing :refer [accessible-squares]]))

(def other-side {:red :blue :blue :red})

;; Routing and accessibility

(defn manhattan [[x y] dist]
  (set (for [d (range 0 (inc dist))
             x' (range (- d) (inc d))
             y' (range (- d) (inc d))
             :when (= d (+ (Math/abs x') (Math/abs y')))]
         [(+ x x') (+ y y')])))

(defn can-move-to
  "Given a game state and a unit, will return a set of every
   grid location that unit can validly move to based on its 
   current movement points and the terrain."
  [game-state {:keys [position move-points movement-table]}]
  (accessible-squares
   position
   move-points
   (->> (manhattan position move-points)
        (field/terrain-map (:field game-state))
        (map-vals movement-table)
        (remove #(nil? (val %))))))

(defn route-cost
  "Given a game state, a unit and a route starting at
   that units location, will return the cost of walking
   that route"
  [game-state {:keys [movement-table position]} route]
  (if (not= position (first route))
    (throw (ex-info "Invalid route, doesn't start with unit's current position" {:position position :route route}))
    (->> (rest route)
         (field/terrain-map (:field game-state))
         (map-vals movement-table)
         (vals)
         (apply +))))

;; Movement stuff

(defn update-move-order
  "A move order has a route, so if there are remaining steps
   in the route the order needs to be updated after a move.
   If there are no steps remaining the order needs to be
   removed completely."
  [game-state]
  (let [[order-type side unit route] (:order game-state)]
    (if (= 1 (count route))
      (dissoc game-state :order)
      (assoc game-state :order [order-type side unit (rest route)]))))

(defn move-order [game-state side unit-id route]
  (let [unit (get-in game-state [side :units unit-id])
        move-cost ((:movement-table unit) (get-in game-state [:field (first route) :terrain]))]
    (cond (unit-in-square game-state (first route))
          (do (println "Cannot move to an occupied square")
              game-state)

          (or (not (can-move? unit))
              (< (:move-points unit) move-cost))
          (do (println "Not enough movement points")
              game-state)

          :else
          (-> game-state
              (assoc-in [side :units unit-id :position] (first route))
              (update-in [side :units unit-id :move-points] - move-cost)
              (update-move-order)))))

;; Combat stuff

(defn update-hp [unit hit-for]
  (update unit :hp - hit-for))

(defn resolve-combat [atkr defdr]
  [(assoc (update-hp atkr (:defence defdr)) :can-attack false)
   (update-hp defdr (int (* (/ (:hp atkr) 10) (:attack atkr))))])

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
        (update-unit u2)
        (dissoc :order))))

(defn end-turn [game-state side]
  (if (= side (:turn game-state))
    (-> game-state
        (assoc :turn (if (= :red side) :blue :red))
        (update-in [side :units] refresh-units)
        (dissoc :order))
    (throw (ex-info "Cannot end turn for this side, not their turn" {:side side}))))

(defn handle-input [game-state order]
  (let [[order-type side unit target] order]
    (case order-type
      :move (move-order game-state side unit target)
      :end-turn (end-turn game-state side)
      :attack (do (println "Attacking") (attack-order game-state side unit target)))))
