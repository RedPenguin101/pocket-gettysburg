(ns general-slim.ui
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.game :as game :refer [tick key-handler coord->px]]
            [general-slim.inputs :as inputs :refer [route-cost]] ;; needed for debug, maybe move this too
            [general-slim.forces :as forces :refer [unit-in-square can-move?]] ;; and this
            ))

;; state and constants

(def debug (atom {}))
(def game-state game/game-state)
(def fps game/fps)
(def horiz-tiles game/horiz-tiles)
(def vert-tiles game/vert-tiles)
(def tile-size game/tile-size)
(def colors game/colors)
(def scale-factor (/ tile-size 100))

(defn scale [x] (int (* scale-factor x)))

;; Handers

(defn setup []
  (q/frame-rate fps)
  (assoc game-state :cursor [5 5]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Drawing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-tile [x y color]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill color)
  (q/rect x y tile-size tile-size))

;; terrain

(defn draw-tree [x y]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill (get-in colors [:terrain :trees]))
  (q/rect (+ x (scale 40)) (+ y (scale 50)) (scale 20) (scale 40))
  (q/triangle (+ x (scale 50)) (+ y (scale 10))
              (+ x (scale 20)) (+ y (scale 70))
              (+ x (scale 80)) (+ y (scale 70))))

(defn draw-mountain [x y]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill (get-in colors [:terrain :mountains]))
  (q/triangle (+ x (scale 50)) (+ y (scale 20))
              (+ x (scale 10)) (+ y (scale 90))
              (+ x (scale 90)) (+ y (scale 90))))

(defn draw-road [x y dirs]
  (doseq [d dirs]
    (q/stroke 0)
    (q/stroke-weight 0)
    (q/fill (get-in colors [:terrain :road]))
    (case d
      :hor (q/rect x (+ y 40) 100 20)
      :vert (q/rect (+ x 40) y 20 100)
      :ne (q/quad (+ x 40) y
                  (+ x 60) y
                  (+ x 100) (+ y 40)
                  (+ x 100) (+ y 60))
      :nw (q/quad (+ x 40) y
                  (+ x 60) y
                  x (+ y 60)
                  x (+ y 40))
      :se (q/quad (+ x 40) (+ y 100)
                  (+ x 60) (+ y 100)
                  (+ x 100) (+ y 60)
                  (+ x 100) (+ y 40))
      :sw (q/quad (+ x 40) (+ y 100)
                  (+ x 60) (+ y 100)
                  x (+ y 40)
                  x (+ y 60)))))

(defn draw-terrain [tiles]
  (doseq [tile tiles]
    (let [[x y] (map coord->px (:grid tile))]
      (case (:terrain tile)
        :trees (draw-tree x y)
        :mountains (draw-mountain x y)
        :road (draw-road x y (:dirs tile))
        nil))))

;; units

(defn draw-unit [{:keys [position id hp]} color]
  (let [x (coord->px (first position))
        y (coord->px (second position))]
    (draw-tile x y color)
    (q/stroke 1)
    (q/stroke-weight 1)
    (q/fill (colors :white))
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (q/text (name id) (+ x (scale 15)) (+ y (scale 30)))
    (q/text-font (q/create-font "Courier New" (scale 20)))
    (q/text (str hp) (+ x (scale 70)) (+ y (scale 90)))))

(defn draw-units [game-state side]
  (doseq [unit (vals (get-in game-state [side :units]))]
    (let [color (cond
                  (not (can-move? unit)) (get-in colors [side :spent])
                  (= (:position unit) (:selected game-state))
                  (get-in colors [side :selected])
                  :else (get-in colors [side :default]))]
      (draw-unit unit color))))

;; other

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

;; menus

(defn draw-turn-indicator [side]
  (q/fill (get-in colors [side :default]))
  (q/rect 0 0 (scale 30) (scale 30)))

(defn draw-menu [game-state]
  (let [x-offset (scale 50) y-offset (scale 50)
        line-offset (scale 35)]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight (scale 6))
    (q/rect x-offset y-offset (scale 300) (scale 300))
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (doseq [[row item] (map-indexed vector (vals (get-in game-state [:menu :options])))]
      (q/text item (+ (scale 20) x-offset) (+ (* row line-offset) (scale 40) y-offset)))
    (q/stroke-weight 1)
    (q/fill (colors :menu-select))
    (q/rect (+ x-offset (scale 10)) (+ (* line-offset (get-in game-state [:menu :selection])) (+ y-offset (scale 10))) (scale 250) (scale 35))))

(defn draw-debug-box [game-state]
  (let [cursor (:cursor game-state)
        unit (unit-in-square game-state cursor)
        selected-unit (unit-in-square game-state (:selected game-state))
        x-offset (scale 3) y-offset (scale 3)
        line-offset (scale 30)]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight (scale 6))
    (q/rect x-offset y-offset (scale 1000) (scale 300))
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (q/text (str "Cursor: " (:cursor game-state) " Selected: " (:selected game-state))
            (+ (scale 25) x-offset) (- (scale 50) y-offset))

    ;; refactor this text stuff
    (when unit
      (q/text (str "CURSOR" (:hp unit) "hp")
              (+ (scale 25) x-offset) (- (+ (* 1 line-offset) (scale 50)) y-offset))
      (q/text (str "Attack option: " (:attack-option game-state))
              (+ (scale 25) x-offset) (- (+ (* 2 line-offset) (scale 50)) y-offset))
      (q/text (str "Move points: " (:move-points unit))
              (+ (scale 25) x-offset) (- (+ (* 3 line-offset) (scale 50)) y-offset))
      (q/text (str "Att/Def: " (:attack unit) "/" (forces/defence-value unit (get-in game-state [:field (:position unit) :terrain])))
              (+ (scale 25) x-offset) (- (+ (* 4 line-offset) (scale 50)) y-offset)))
    (when selected-unit
      (q/text (str "SELECTED" (:hp selected-unit) "hp")
              (+ (scale 25) x-offset) (- (+ (* 1 line-offset) (scale 50)) y-offset))
      (q/text (str "Attack option: " (:attack-option game-state))
              (+ (scale 25) x-offset) (- (+ (* 2 line-offset) (scale 50)) y-offset))
      (q/text (str "Move points: " (:move-points selected-unit))
              (+ (scale 25) x-offset) (- (+ (* 3 line-offset) (scale 50)) y-offset))
      (q/text (str "Att/Def: " (:attack selected-unit) "/" (forces/defence-value selected-unit (get-in game-state [:field (:position selected-unit) :terrain])))
              (+ (scale 25) x-offset) (- (+ (* 4 line-offset) (scale 50)) y-offset)))
    (when (:route-selection game-state)
      (q/text (str "Coords: " cursor)
              (+ (scale 25) x-offset) (- (+ (* 5 line-offset) (scale 50)) y-offset))
      (q/text (str "Route: " (:route game-state))
              (+ (scale 25) x-offset) (- (+ (* 6 line-offset) (scale 50)) y-offset))
      (q/text (str "Route cost: " (route-cost game-state selected-unit (reverse (:route game-state))))
              (+ (scale 25) x-offset) (- (+ (* 7 line-offset) (scale 50)) y-offset)))))

(defn draw-state [game-state]
  (q/background 240)
  (draw-terrain (vals (:field game-state)))
  (when (:route-selection game-state) (draw-routing (:route game-state)))
  (draw-units game-state :red)
  (draw-units game-state :blue)
  (when (:highlight game-state) (draw-highlights (:highlight game-state)))
  (draw-cursor (:cursor game-state))
  (draw-turn-indicator (:turn game-state))
  (when (:menu game-state) (draw-menu game-state))
  (when (:debug game-state) (draw-debug-box game-state)))

(comment)
(q/defsketch game
  :host "map"
  :size [(* horiz-tiles tile-size) (* vert-tiles tile-size)]
  :setup setup
  :settings #(q/smooth 2)
  :draw draw-state
  :update #(do (reset! debug %) (tick %))
  :key-pressed key-handler
  :middleware [m/fun-mode])

(dissoc @debug :field)