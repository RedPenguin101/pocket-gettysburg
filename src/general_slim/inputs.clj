(ns general-slim.inputs
  (:require [clojure.set :refer [intersection difference]]
            [general-slim.utils :as u :refer [dissoc-in map-vals opposing-dirs relative-coord relative-position]]
            [general-slim.route-calc :as routing :refer [accessible-squares]]
            [general-slim.forces :as forces :refer [can-move? refresh-units]]
            [general-slim.field :as field]
            [general-slim.combat :as combat]
            [general-slim.viewsheds :as vs]))

;; General
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def other-side {:red :blue :blue :red})

(defn update-unit [game-state unit]
  (if (zero? (:soldiers unit))
    (dissoc-in game-state [(:side unit) :units] (:id unit))
    (assoc-in game-state [(:side unit) :units (:id unit)] unit)))

;; Routing and accessibility
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-move-to
  "Given a game state and a unit, will return a set of every
   grid location that unit can validly move to based on its 
   current movement points and the terrain."
  [game-state {:keys [position move-points movement-table]}]
  (accessible-squares
   position
   move-points
   (->> (u/manhattan position (+ 3 move-points)) ;; might have to expand this, maybe do some cacheing
        (remove (forces/occupied-grids game-state))
        (cons position) ;; yuk
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

;; Movement order execution
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-attack-option
  "After a move has been done, calculate whether any enemy
   units are in adjacent locations (and so are attackable)"
  [game-state side unit-id unit-loc]
  (let [attacking-unit (get-in game-state [side :units unit-id])
        targets (intersection (forces/occupied-grids game-state (other-side side))
                              (get attacking-unit :viewshed)
                              (u/manhattan unit-loc 1))]
    (if (or (empty? targets) (#{:general} (:unit-type attacking-unit)))
      (assoc game-state :attack-option :no-targets)
      (assoc game-state :attack-option [side unit-id unit-loc targets]))))

(defn update-move-order
  "A move order has a route, so if there are remaining steps
   in the route the order needs to be updated after a move.
   If there are no steps remaining the order needs to be
   removed completely."
  [game-state]
  (let [[order-type side unit-id route] (:current-order game-state)]
    (if (= 1 (count route))
      (-> game-state
          (dissoc :current-order)
          (add-attack-option side unit-id (first route))
          (assoc-in [side :units unit-id :move-over] true))
      (-> game-state
          (assoc :current-order [order-type side unit-id (rest route)])))))

(defn execute-move-order
  [game-state move-type side unit-id route]
  (let [unit (get-in game-state [side :units unit-id])
        target-occupied? (forces/unit-at-location game-state (first route))
        move-cost (if (= :retreat move-type) 0 ((:movement-table unit) (get-in game-state [:field (first route) :terrain])))]
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
              (vs/update-viewshed unit-id)
              (update-move-order)))))

;; Retreating
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-retreat-square
  [retreater-pos enemy-pos occupied-squares]
  (let [retreatable-squares (difference (u/manhattan retreater-pos 1) occupied-squares)
        rel-pos-of-enemy (relative-position retreater-pos enemy-pos)
        preferred-retreat (relative-coord retreater-pos (opposing-dirs rel-pos-of-enemy))]
    (cond (empty? retreatable-squares) nil
          (retreatable-squares preferred-retreat) preferred-retreat
          :else (rand-nth (vec retreatable-squares)))))

(defn add-retreat-order [game-state side unit-id retreat-square]
  (if (get-in game-state [side :units unit-id :soldiers])
    (update game-state :order-queue conj [:retreat side unit-id [retreat-square]])
    game-state))

;; Combat order execution
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-combat-outcome [game-state resolution attacker defender]
  (case resolution
    :turn-finished      game-state
    :attacker-retreats (add-retreat-order game-state (:side attacker) (:id attacker) (find-retreat-square (:position attacker) (:position defender) (forces/occupied-grids game-state)))
    :defender-retreats (add-retreat-order game-state (:side defender) (:id defender) (find-retreat-square (:position defender) (:position attacker) (forces/occupied-grids game-state)))))

(defn execute-attack-order
  [game-state my-side my-unit-id enemy-unit-id]
  (let [my-unit (get-in game-state [my-side :units my-unit-id])
        enemy-unit (get-in game-state [(other-side my-side) :units enemy-unit-id])
        [resolution u1 u2] (combat/resolve-combat!
                            (assoc my-unit :terrain (field/terrain-at (:field game-state) (:position my-unit)))
                            (assoc enemy-unit :terrain (field/terrain-at (:field game-state) (:position enemy-unit))))]
    (-> game-state
        (dissoc :current-order)
        (update-unit u1)
        (update-unit u2)
        (assoc-in [my-side :units my-unit-id :move-over] true)
        (handle-combat-outcome resolution u1 u2))))


;; Other exeuction (End turn really shouldn't be an order!)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn end-turn [game-state side]
  (println "Ending turn")
  (if (= side (:turn game-state))
    (-> game-state
        (assoc :turn (if (= :red side) :blue :red))
        (update-in [side :units] refresh-units)
        (update-in [(other-side side) :units] refresh-units)
        (dissoc :current-order)
        (update :turn-number inc))
    (throw (ex-info "Cannot end turn for this side, not their turn" {:side side}))))

(defn handle-input [game-state]
  (cond (:current-order game-state)
        (let [[order-type side unit target] (:current-order game-state)]
          (case order-type
            :move (execute-move-order game-state order-type side unit target)
            :retreat (execute-move-order game-state order-type side unit target)
            :end-turn (end-turn game-state side)
            :attack (execute-attack-order game-state side unit target)))
        (not-empty (:order-queue game-state))
        (-> game-state
            (assoc :current-order (first (:order-queue game-state)))
            (update :order-queue rest))

        :else (do (println "Erroneous input handle")
                  game-state)))
