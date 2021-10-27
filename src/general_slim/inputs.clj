(ns general-slim.inputs
  (:require [clojure.set :refer [intersection difference]]
            [general-slim.forces :as forces :refer [can-move? unit-in-square refresh-units occupied-grids]]
            [general-slim.utils :refer [dissoc-in map-vals opposing-dirs relative-coord relative-position]]
            [general-slim.field :as field]
            [general-slim.combat :as combat]
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
  (let [[order-type side unit-id route] (:current-order game-state)]
    (if (= 1 (count route))
      (-> game-state
          (dissoc :current-order)
          (add-attack-option side unit-id (first route)))
      (assoc game-state :current-order [order-type side unit-id (rest route)]))))

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

(defn find-retreat-square [retreater-pos enemy-pos occupied-squares]
  (let [retreatable-squares (difference (manhattan retreater-pos 1) occupied-squares)
        rel-pos-of-enemy (relative-position retreater-pos enemy-pos)
        preferred-retreat (relative-coord retreater-pos (opposing-dirs rel-pos-of-enemy))]
    #_(do (println "retreater square" retreater-pos)
          (println "Enemy pos" enemy-pos)
          (println "Rel Pos of enemy" rel-pos-of-enemy)
          (println "retreatable squares" retreatable-squares)
          (println "preferred-retreat" preferred-retreat))
    (cond (empty? retreatable-squares) nil
          (retreatable-squares preferred-retreat) preferred-retreat
          :else (rand-nth (vec retreatable-squares)))))

(defn add-retreat-order [game-state retreating-unit retreat-square]
  (println "retreating side:" (:side retreating-unit))
  (update game-state :order-queue conj [:move (:side retreating-unit) (:id retreating-unit) [retreat-square]]))

(defn handle-retreat [game-state resolution attacker defender]
  (println "Combat resolution:" resolution)
  (case resolution
    :turn-finished game-state
    :attacker-retreats (add-retreat-order game-state attacker (find-retreat-square (:position attacker) (:position defender) (occupied-grids game-state)))
    :defender-retreats (add-retreat-order game-state defender (find-retreat-square (:position defender) (:position attacker) (occupied-grids game-state)))))

(defn update-unit [game-state unit]
  (if (zero? (:soldiers unit))
    (dissoc-in game-state [(:side unit) :units] (:id unit))
    (assoc-in game-state [(:side unit) :units (:id unit)] unit)))

(defn attack-order
  [game-state my-side my-unit-id enemy-unit-id]
  (let [my-unit (get-in game-state [my-side :units my-unit-id])
        enemy-unit (get-in game-state [(other-side my-side) :units enemy-unit-id])
        [resolution u1 u2] (combat/resolve-combat
                            (assoc my-unit :terrain (field/terrain-at (:field game-state) (:position my-unit)))
                            (assoc enemy-unit :terrain (field/terrain-at (:field game-state) (:position enemy-unit))))]
    (-> game-state
        (update-unit u1)
        (update-unit u2)
        (assoc-in [my-side :units my-unit-id :can-attack] false)
        (dissoc :current-order)
        (handle-retreat resolution u1 u2))))

(defn end-turn [game-state side]
  (println "Ending turn")
  (if (= side (:turn game-state))
    (-> game-state
        (assoc :turn (if (= :red side) :blue :red))
        (update-in [side :units] refresh-units)
        (dissoc :current-order))
    (throw (ex-info "Cannot end turn for this side, not their turn" {:side side}))))

(defn handle-input [game-state]
  #_(println "==Input handle:")
  (println "Current-order: " (:current-order game-state))
  #_(println "Order queue: " (:order-queue game-state))
  (cond (:current-order game-state)
        (let [[order-type side unit target] (:current-order game-state)]
          (case order-type
            :move (move-order game-state side unit target)
            :end-turn (end-turn game-state side)
            :attack (do (println "Attacking") (attack-order game-state side unit target))))
        (not-empty (:order-queue game-state))
        (-> game-state
            (assoc :current-order (first (:order-queue game-state)))
            (update :order-queue rest))

        :else (do (println "Erroneous input handle")
                  game-state)))
