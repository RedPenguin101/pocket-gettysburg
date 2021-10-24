(ns general-slim.ui
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.game :refer [tick]]
            [general-slim.inputs :as inputs :refer [can-move-to route-cost]]
            [general-slim.forces :as forces :refer [unit-in-square can-move?]]
            [general-slim.game-states :as gs]))

;; state and constants

(def debug (atom {}))
(def game-state gs/aw-ft1)
(def fps 30)
(let [[x y] (:field-size game-state)]
  (def horiz-tiles x)
  (def vert-tiles y))
(def tile-size 100)
(def colors {:cursor [183 183 183 75]
             :map-highlight [220 220 220 75]
             :routing [101 252 90 75]
             :terrain {:trees [36 119 23]
                       :mountains [124 117 104]
                       :road [140 101 33]}
             :red {:default [211 61 61]
                   :spent [150 42 42]
                   :selected [252 126 126]}
             :blue {:default [61 106 211]
                    :spent [37 68 142]
                    :selected [106 149 252]}
             :white [252 252 252]
             :menu-select [183 183 183 75]})

;; utils

(defn up [[x y]] [x (dec y)])
(defn down [[x y]] [x (inc y)])
(defn left [[x y]] [(dec x) y])
(defn right [[x y]] [(inc x) y])

(def grid-moves {:up up :down down :left left :right right})

(defn bound [[x y]]
  [(min (max 0 x) (dec horiz-tiles))
   (min (max 0 y) (dec vert-tiles))])

(defn coord->px [x] (int (* tile-size x)))

;; Handers

(defn setup []
  (q/frame-rate fps)
  (assoc game-state :cursor [5 5]))

(defn handle-cursor-for-move [game-state mv-fn]
  (let [new-cursor ((comp bound mv-fn) (:cursor game-state))
        selected-unit (unit-in-square game-state (:selected game-state))]
    (cond (= new-cursor (:cursor game-state))
          game-state

          (not (:route-selection game-state))
          (assoc game-state :cursor new-cursor)

          ;; can't move through units
          (and (unit-in-square game-state new-cursor)
               (not= new-cursor (:position selected-unit)))
          game-state

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

(defn handle-attack-selection [game-state]
  (let [[side attacker-id] (:attack-mode game-state)
        defender-id (:id (unit-in-square game-state (:cursor game-state)))]
    (-> game-state
        (assoc :order [:attack side attacker-id defender-id])
        (dissoc :attack-mode :selected))))

(defn handle-selection-for-move [game-state]
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

      ;; MOVE if there's a selected unit and the target ISN'T an enemy
      (and selected-unit? (not unit-under-cursor?))

      (-> game-state
          (assoc :order [:move my-side (:id selected-unit?) (reverse (butlast (:route game-state)))])
          (assoc :selected (first (:route game-state)))
          (dissoc :highlight :route-selection :route))

      :else (do (println "Selection fall through") game-state))))

(defn handle-menu-cursor [game-state dir]
  (let [menu-items (count (get-in game-state [:menu :options]))]
    (case dir
      :down (update-in game-state [:menu :selection] #(mod (inc %) menu-items))
      :up (update-in game-state [:menu :selection] #(mod (dec %) menu-items))
      game-state)))

(defn handle-menu-selection [game-state]
  (let [selected-option (nth (keys (get-in game-state [:menu :options]))
                             (get-in game-state [:menu :selection]))]
    (println "selected" selected-option)
    (case (get-in game-state [:menu :name])
      :attack-menu
      (if (= :wait selected-option)
        (dissoc game-state :selected :highlight :attack-option :menu)
        (-> game-state
            (assoc :cursor (first (last (:attack-option game-state)))
                   :attack-mode (:attack-option game-state))
            (dissoc :menu :attack-option)))

      game-state)))

(defn handle-attack-cursor [game-state dir]
  (let [attempted-selection ((grid-moves dir) (:selected game-state))]
    (if ((last (:attack-mode game-state)) attempted-selection)
      (assoc game-state :cursor attempted-selection)
      game-state)))

(defn handle-selection [game-state]
  (cond (:menu game-state) (handle-menu-selection game-state)
        (:attack-mode game-state) (handle-attack-selection game-state)
        :else (handle-selection-for-move game-state)))

(defn handle-cursor [game-state dir]
  (cond (:menu game-state) (handle-menu-cursor game-state dir)
        (:attack-mode game-state) (handle-attack-cursor game-state dir)
        :else (handle-cursor-for-move game-state (dir grid-moves))))

(defn key-handler [game-state event]
  (case (:key event)
    :up (handle-cursor game-state :up)
    :down (handle-cursor game-state :down)
    :left (handle-cursor game-state :left)
    :right (handle-cursor game-state :right)
    :space (handle-selection game-state)
    :d (update game-state :debug not)
    :c (assoc game-state :order [:end-turn (:turn game-state)])
    :q (dissoc game-state :route-selection :route :selected :highlight)
    game-state))

(defn tick-wrap [game-state]
  (let [next-state (tick game-state)]
    (if (and (not (:menu next-state)) (:attack-option next-state))
      (assoc next-state :menu {:name :attack-menu
                               :options {:attack "> Attack"
                                         :wait "> Wait"}
                               :selection 0})
      next-state)))

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
  (q/rect 0 0 30 30))

(defn draw-menu [game-state]
  (let [cursor (:cursor game-state)
        unit (unit-in-square game-state cursor)
        selected-unit (unit-in-square game-state (:selected game-state))
        x-offset 50 y-offset 50
        line-offset 35]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight 6)
    (q/rect x-offset y-offset 300 300)
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" 30))
    (doseq [[row item] (map-indexed vector (vals (get-in game-state [:menu :options])))]
      (q/text item (+ 20 x-offset) (+ (* row line-offset) 40 y-offset)))
    (q/stroke-weight 1)
    (q/fill (colors :menu-select))
    (q/rect (+ x-offset 10) (+ (* line-offset (get-in game-state [:menu :selection])) (+ y-offset 10)) 250 35)))

(defn draw-debug-box [game-state]
  (let [cursor (:cursor game-state)
        unit (unit-in-square game-state cursor)
        selected-unit (unit-in-square game-state (:selected game-state))
        x-offset 3 y-offset 3
        line-offset 30]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight 6)
    (q/rect x-offset y-offset 1000 300)
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" 30))
    (q/text (str "Cursor: " (:cursor game-state) " Selected: " (:selected game-state))
            (+ 25 x-offset) (- 50 y-offset))
    (when unit
      (q/text (str "CURSOR" (:hp unit) "hp")
              (+ 25 x-offset) (- (+ (* 1 line-offset) 50) y-offset))
      (q/text (str "Attack option: " (:attack-option game-state))
              (+ 25 x-offset) (- (+ (* 2 line-offset) 50) y-offset))
      (q/text (str "Move points: " (:move-points unit))
              (+ 25 x-offset) (- (+ (* 3 line-offset) 50) y-offset))
      (q/text (str "Att/Def: " (:attack unit) "/" (forces/defence-value unit (get-in game-state [:field (:position unit) :terrain])))
              (+ 25 x-offset) (- (+ (* 4 line-offset) 50) y-offset)))
    (when selected-unit
      (q/text (str "SELECTED" (:hp selected-unit) "hp")
              (+ 25 x-offset) (- (+ (* 1 line-offset) 50) y-offset))
      (q/text (str "Attack option: " (:attack-option game-state))
              (+ 25 x-offset) (- (+ (* 2 line-offset) 50) y-offset))
      (q/text (str "Move points: " (:move-points selected-unit))
              (+ 25 x-offset) (- (+ (* 3 line-offset) 50) y-offset))
      (q/text (str "Att/Def: " (:attack selected-unit) "/" (forces/defence-value selected-unit (get-in game-state [:field (:position selected-unit) :terrain])))
              (+ 25 x-offset) (- (+ (* 4 line-offset) 50) y-offset)))
    (when (:route-selection game-state)
      (q/text (str "Coords: " cursor)
              (+ 25 x-offset) (- (+ (* 5 line-offset) 50) y-offset))
      (q/text (str "Route: " (:route game-state))
              (+ 25 x-offset) (- (+ (* 6 line-offset) 50) y-offset))
      (q/text (str "Route cost: " (route-cost game-state selected-unit (reverse (:route game-state))))
              (+ 25 x-offset) (- (+ (* 7 line-offset) 50) y-offset)))))

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
  :update #(do (reset! debug %) (tick-wrap %))
  :key-pressed key-handler
  :middleware [m/fun-mode])

(dissoc @debug :field)