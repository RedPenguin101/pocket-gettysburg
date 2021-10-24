(ns general-slim.ui
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.game :refer [tick]]
            [general-slim.inputs :as inputs :refer [can-move-to route-cost]]
            [general-slim.forces :as forces :refer [unit-in-square can-move?]]
            [general-slim.game-states :as gs]))

;; state and constants

(def debug (atom {}))
(def game-state gs/mountains)
(def fps 30)
(def tiles 10)
(def tile-size 100)
(def colors {:cursor [183 183 183 75]
             :map-highlight [220 220 220 50]
             :routing [101 252 90 75]
             :terrain {:trees [36 119 23]
                       :mountains [124 117 104]}
             :red {:default [211 61 61]
                   :spent [150 42 42]
                   :selected [252 126 126]}
             :blue {:default [61 106 211]
                    :spent [37 68 142]
                    :selected [106 149 252]}
             :white [252 252 252]})

;; utils

(defn up [[x y]] [x (dec y)])
(defn down [[x y]] [x (inc y)])
(defn left [[x y]] [(dec x) y])
(defn right [[x y]] [(inc x) y])

(defn bound [[x y]]
  [(min (max 0 x) (dec tiles))
   (min (max 0 y) (dec tiles))])

(defn coord->px [x] (int (* tile-size x)))

;; Handers

(defn setup []
  (q/frame-rate fps)
  (assoc game-state :cursor [5 5]))

(defn handle-selection [game-state]
  (let [cursor (:cursor game-state)
        unit-under-cursor? (unit-in-square game-state cursor)
        my-side (:turn game-state)
        selected? (:selected game-state)
        selected-unit? (unit-in-square game-state selected?)]
    (cond
      ;; If no selection, and trying to select your unit, select and turn on route selection
      (and (not selected?)
           (= my-side (:side unit-under-cursor?))
           (can-move? unit-under-cursor?))
      (assoc game-state
             :route-selection true
             :route (list cursor)
             :selected cursor
             :highlight (can-move-to game-state unit-under-cursor?))

      ;; If there's a selected unit and the target is an enemy unit, attack it
      (and selected-unit? unit-under-cursor? (not= my-side (:side unit-under-cursor?)))
      (dissoc
       (assoc game-state :order [:attack my-side (:id selected-unit?) (:id unit-under-cursor?)])
       :selected :highlight :route-selection :route)

      ;; if there's a selected unit and the target ISN'T an enemy, move
      (and selected-unit? (not unit-under-cursor?))
      (dissoc
       (assoc game-state :order [:move my-side (:id selected-unit?) (reverse (butlast (:route game-state)))])
       :selected :highlight :route-selection :route)

      :else (do (println "Selection fall through") game-state))))

(defn cursor-move [game-state mv-fn]
  (let [new-cursor ((comp bound mv-fn) (:cursor game-state))
        selected-unit (unit-in-square game-state (:selected game-state))]
    (cond (= new-cursor (:cursor game-state))
          game-state

          (not (:route-selection game-state))
          (assoc game-state :cursor new-cursor)

          ;; can always back out a selection
          (= new-cursor (second (:route game-state)))
          (-> game-state
              (assoc :cursor new-cursor)
              (update :route rest))

          (<= (route-cost game-state selected-unit (reverse (conj (:route game-state) new-cursor)))
              (:move-points selected-unit))
          (-> game-state
              (assoc :cursor new-cursor)
              (update :route conj new-cursor))

          :else game-state)))

(defn key-handler [game-state event]
  (case (:key event)
    :up (cursor-move game-state up)
    :down (cursor-move game-state down)
    :left (cursor-move game-state left)
    :right (cursor-move game-state right)
    :space (handle-selection game-state)
    :d (update game-state :debug not)
    :c (assoc game-state :order [:end-turn (:turn game-state)])
    :q (dissoc game-state :route-selection :route :selected :highlight)
    game-state))

;; Drawing

(defn draw-tile [x y color]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill color)
  (q/rect x y tile-size tile-size))

(defn draw-tree [x y]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill (get-in colors [:terrain :trees]))
  (q/rect (+ x 40) (+ y 50) 20 40)
  (q/triangle (+ x 50) (+ y 10)
              (+ x 20) (+ y 70)
              (+ x 80) (+ y 70)))

(defn draw-mountain [x y]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill (get-in colors [:terrain :mountains]))
  (q/triangle (+ x 50) (+ y 20)
              (+ x 10) (+ y 90)
              (+ x 90) (+ y 90)))

(defn draw-terrain [tiles]
  (doseq [tile tiles]
    (let [[x y] (:grid tile)]
      (case (:terrain tile)
        :trees (draw-tree (coord->px x) (coord->px y))
        :mountains (draw-mountain (coord->px x) (coord->px y))
        nil))))

(defn draw-unit [{:keys [position id hp]} color]
  (let [x (coord->px (first position))
        y (coord->px (second position))]
    (draw-tile x y color)
    (q/stroke 1)
    (q/stroke-weight 1)
    (q/fill (colors :white))
    (q/text-font (q/create-font "Courier New" 30))
    (q/text (name id) (+ x 15) (+ y 30))
    (q/text-font (q/create-font "Courier New" 20))
    (q/text (str hp) (+ x 70) (+ y 90))))

(defn draw-units [game-state side]
  (doseq [unit (vals (get-in game-state [side :units]))]
    (let [color (cond
                  (not (can-move? unit)) (get-in colors [side :spent])
                  (= (:position unit) (:selected game-state))
                  (get-in colors [side :selected])
                  :else (get-in colors [side :default]))]
      (draw-unit unit color))))

(defn draw-cursor [[x y]]
  (draw-tile (coord->px x) (coord->px y) (colors :cursor)))

(defn draw-highlights [coords]
  (doseq [[x y] coords]
    (draw-tile (coord->px x) (coord->px y)
               (colors :map-highlight))))

(defn draw-routing [coords]
  (doseq [[x y] coords]
    (draw-tile (coord->px x) (coord->px y)
               (colors :routing))))

(defn draw-turn-indicator [side]
  (q/fill (get-in colors [side :default]))
  (q/rect 0 0 30 30))

(defn draw-debug-box [game-state]
  (let [[x y :as cursor] (:cursor game-state)
        unit (unit-in-square game-state cursor)
        x-offset (if (and (>= x 5) (<= y 2)) 3 497) y-offset 3
        line-offset 30]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight 6)
    (q/rect x-offset y-offset 500 300)
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" 30))
    (when unit
      (q/text (str "Can attack: " (:can-attack unit))
              (+ 25 x-offset) (- (+ (* 2 line-offset) 50) y-offset))
      (q/text (str "Move points: " (:move-points unit))
              (+ 25 x-offset) (- (+ (* 3 line-offset) 50) y-offset))
      (q/text (str (name (:id unit)))
              (+ 25 x-offset) (- 50 y-offset))
      (q/text (str (:hp unit) "hp")
              (+ 25 x-offset) (- (+ (* 1 line-offset) 50) y-offset))
      (q/text (str "Att/Def: " (:attack unit) "/" (forces/defence-value unit (get-in game-state [:field (:position unit) :terrain])))
              (+ 25 x-offset) (- (+ (* 4 line-offset) 50) y-offset)))
    (when (:route-selection game-state)
      (q/text (str "Coords: " cursor)
              (+ 25 x-offset) (- (+ (* 5 line-offset) 50) y-offset))
      (q/text (str "Route sel: " (:route-selection game-state))
              (+ 25 x-offset) (- (+ (* 6 line-offset) 50) y-offset))
      (q/text (str "Route: " (:route game-state))
              (+ 25 x-offset) (- (+ (* 7 line-offset) 50) y-offset)))))

(defn draw-state [game-state]
  (q/background 240)
  (draw-terrain (vals (:field game-state)))
  (when (:highlight game-state) (draw-highlights (:highlight game-state)))
  (when (:route-selection game-state) (draw-routing (:route game-state)))
  (draw-units game-state :red)
  (draw-units game-state :blue)
  (draw-cursor (:cursor game-state))
  (draw-turn-indicator (:turn game-state))
  (when (:debug game-state) (draw-debug-box game-state)))

(comment)
(q/defsketch game
  :host "map"
  :size [(* tiles tile-size) (* tiles tile-size)]
  :setup setup
  :settings #(q/smooth 2)
  :draw draw-state
  :update #(do (reset! debug %) (tick %))
  :key-pressed key-handler
  :middleware [m/fun-mode])

(dissoc @debug :field)