(ns general-slim.inputs
  (:require [clojure.set :refer [intersection]]
            [general-slim.forces :as forces :refer [can-move? unit-in-square refresh-units occupied-grids]]
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
   (->> (manhattan position (+ 3 move-points)) ;; might have to expand this, maybe do some cacheing
        (remove (occupied-grids game-state))
        (cons position) ;; yuk
        (field/terrain-map (:field game-state))
        (map-vals movement-table)
        (remove #(nil? (val %))))))

(comment
  ;; get last run gamestate and fiddle with it
  (def gs @general-slim.ui/debug)
  gs
  (sort (let [unit (unit-in-square gs [1 7])]
          (can-move-to gs unit))))

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

(defn add-attack-option [game-state side unit-id unit-loc]
  (let [targets (intersection (occupied-grids game-state (other-side side)) (manhattan unit-loc 1))]
    (if (empty? targets)
      (assoc game-state :attack-option :no-targets)
      (assoc game-state :attack-option [side unit-id targets]))))

(defn update-move-order
  "A move order has a route, so if there are remaining steps
   in the route the order needs to be updated after a move.
   If there are no steps remaining the order needs to be
   removed completely."
  [game-state]
  (let [[order-type side unit-id route] (:order game-state)]
    (if (= 1 (count route))
      (-> game-state
          (dissoc :order)
          (add-attack-option side unit-id (first route)))
      (assoc game-state :order [order-type side unit-id (rest route)]))))

(defn move-order [game-state side unit-id route]
  (let [unit (get-in game-state [side :units unit-id])
        target-occupied? (unit-in-square game-state (first route))
        move-cost ((:movement-table unit) (get-in game-state [:field (first route) :terrain]))]
    (cond (and target-occupied? (not= target-occupied? unit))
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

(defn update-hp [unit hit-for terrain]
  (update unit :hp - (max 0 (- hit-for (get-in unit [:terrain-defense terrain])))))

(defn damage [unit offense?]
  (int (/ (* ((if offense? :offensive-power :defensive-power) unit) (:hp unit)) (:max-hp unit))))

(defn resolve-combat [atkr defdr a-terrain d-terrain]
  [(assoc (update-hp atkr (damage defdr false) a-terrain) :can-attack false)
   (update-hp defdr (damage atkr true) d-terrain)])

(defn update-unit [game-state unit]
  (if (> (:hp unit) 0)
    (assoc-in game-state [(:side unit) :units (:id unit)] unit)
    (dissoc-in game-state [(:side unit) :units] (:id unit))))

(defn attack-order
  [game-state my-side my-unit-id enemy-unit-id]
  (let [my-unit (get-in game-state [my-side :units my-unit-id])
        my-terrain (get-in game-state [:field (:position my-unit) :terrain])
        enemy-unit (get-in game-state [(other-side my-side) :units enemy-unit-id])
        enemy-terrain (get-in game-state [:field (:position enemy-unit) :terrain])
        [u1 u2] (resolve-combat my-unit enemy-unit my-terrain enemy-terrain)]
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
