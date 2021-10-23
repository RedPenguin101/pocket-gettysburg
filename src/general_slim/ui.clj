(ns general-slim.ui
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.game :refer [tick]]
            [general-slim.field :as field] ;; just for testing
            [general-slim.forces :as forces :refer [make-unit unit-in-square can-move?]]))

(def game-state {:field (field/flat-field 10 10)
                 :red forces/red
                 :blue forces/blue
                 :turn :red
                 :turn-number 0})

(def ready-to-attack {:field (field/flat-field 10 10)
                      :red {:units {:inf1 (make-unit :infantry :red :inf1 [6 6])
                                    :cav1 (make-unit :cavalry :red :cav1 [3 3])}}
                      :blue {:units {:inf1 (make-unit :infantry :blue :inf1 [7 6])}}
                      :turn :red
                      :turn-number 0
                      :cursor [5 5]})

(def debug (atom {}))
(def grid-size 10)
(def cell-size (quot 1000 grid-size))
(def colors {:cursor [183 183 183 75]
             :map-highlight [220 220 220]
             :routing [101 252 90]
             :red {:default [211 61 61]
                   :spent [150 42 42]
                   :selected [252 126 126]}
             :blue {:default [61 106 211]
                    :spent [37 68 142]
                    :selected [106 149 252]}})

(defn manhattan [[x y] dist]
  (set (for [d (range 0 (inc dist))
             x' (range (- d) (inc d))
             y' (range (- d) (inc d))
             :when (= d (+ (Math/abs x') (Math/abs y')))]
         [(+ x x') (+ y y')])))

(defn up [[x y]] [x (dec y)])
(defn down [[x y]] [x (inc y)])
(defn left [[x y]] [(dec x) y])
(defn right [[x y]] [(inc x) y])

(defn bound [[x y]]
  [(min (max 0 x) (dec grid-size))
   (min (max 0 y) (dec grid-size))])

(defn coord->px [x] (int (* cell-size x)))

(defn setup []
  (q/frame-rate 30)
  ready-to-attack)

(defn draw-tile [x y color]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill color)
  (q/rect x y cell-size cell-size))

(defn draw-unit [{:keys [position id]} color]
  (let [x (coord->px (first position))
        y (coord->px (second position))]
    (draw-tile x y color)
    (q/stroke 1)
    (q/stroke-weight 1)
    (q/fill [255 255 255])
    (q/text-font (q/create-font "Courier New" 40))
    (q/text (name id) (+ x 10) (+ y 60))))

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

(defn draw-status-box [game-state]
  (let [[x y :as cursor] (:cursor game-state)
        unit (unit-in-square game-state cursor)
        x-offset (if (and (>= x 5) (<= y 2)) 3 497) y-offset 3
        line-offset 30]
    (q/stroke 1)
    (q/fill [252 252 252])
    (q/stroke-weight 6)
    (q/rect x-offset y-offset 500 300)
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" 30))
    (q/text (str "Turn: " (name (:turn game-state)))
            (+ 25 x-offset) (- 50 y-offset))
    (q/text (str "Coords: " cursor)
            (+ 25 x-offset) (- (+ line-offset 50) y-offset))
    (q/text (str "Route sel: " (:route-selection game-state))
            (+ 25 x-offset) (- (+ (* 2 line-offset) 50) y-offset))
    (q/text (str "Route: " (:route game-state))
            (+ 25 x-offset) (- (+ (* 3 line-offset) 50) y-offset))
    #_(when unit
        (q/text (str (name (:id unit)) ": " (:hp unit))
                (+ 25 x-offset) (- (+ (* 2 line-offset) 50) y-offset))
        (q/text (str "Can attack: " (:can-attack unit))
                (+ 25 x-offset) (- (+ (* 3 line-offset) 50) y-offset))
        (q/text (str "Move points: " (:move-points unit))
                (+ 25 x-offset) (- (+ (* 4 line-offset) 50) y-offset)))))

(defn draw-state [game-state]
  (q/background 240)
  (when (:highlight game-state) (draw-highlights (:highlight game-state)))
  (when (:route-selection game-state) (draw-routing (:route game-state)))
  (draw-units game-state :red)
  (draw-units game-state :blue)
  (draw-cursor (:cursor game-state))
  (draw-status-box game-state))

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
             :highlight (manhattan cursor (:move-points unit-under-cursor?)))

      ;; If there's a selected unit and the target is an enemy unit, attack it
      (and selected-unit? unit-under-cursor? (not= my-side (:side unit-under-cursor?)))
      (dissoc (assoc game-state :order [:attack my-side (:id selected-unit?) (:id unit-under-cursor?)]) :selected :highlight)

      ;; if there's a selected unit and the target ISN'T an enemy, move
      (and selected-unit? (not unit-under-cursor?))
      (dissoc
       (assoc game-state :order [:move my-side (:id selected-unit?) (reverse (butlast (:route game-state)))])
       :selected :highlight :route-selection :route)

      :else (do (println "Selection fall through") game-state))))

(defn update-route [route new-coord]
  (cond ((manhattan (last route) 1) new-coord)
        (list new-coord (last route))
        (= new-coord (second route))
        (rest route)
        :else (conj route new-coord)))

(defn cursor-move [game-state mv-fn]
  (let [new-cursor ((comp bound mv-fn) (:cursor game-state))]
    (if (:route-selection game-state)
      (if ((:highlight game-state) new-cursor)
        (-> game-state
            (assoc :cursor new-cursor)
            (update :route update-route new-cursor))
        game-state)
      (assoc game-state :cursor new-cursor))))

(defn key-handler [game-state event]
  (case (:key event)
    :up (cursor-move game-state up)
    :down (cursor-move game-state down)
    :left (cursor-move game-state left)
    :right (cursor-move game-state right)
    :space (handle-selection game-state)
    :c (assoc game-state :order [:end-turn (:turn game-state)])
    game-state))

(comment)
(q/defsketch game
  :host "map"
  :size [1000 1000]
  :setup setup
  :settings #(q/smooth 2)
  :draw draw-state
  :update #(do (reset! debug %) (tick %))
  :key-pressed key-handler
  :middleware [m/fun-mode])

(dissoc @debug :field)