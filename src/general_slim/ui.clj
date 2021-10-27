(ns general-slim.ui
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.utils :refer [update-vals]]
            [general-slim.game :as game :refer [tick key-handler coord->px]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; state and constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def debug (atom {}))
(def game-state game/game-state)
(def fps game/fps)
(def horiz-tiles game/horiz-tiles)
(def vert-tiles game/vert-tiles)
(def tile-size game/tile-size)
(def unit-size (int (* 2/3 tile-size)))
(def colors game/colors)
(def scale-factor (/ tile-size 100))

(defn scale [x] (int (* scale-factor x)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sprites
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-images [images]
  (if (every? q/loaded? images)
    nil
    (recur images)))

(defn resize-images [images w h]
  (doseq [i images]
    (q/resize i w h)))

(defn load-sprites []
  (let [infantry (q/load-image "resources/sprites/infantry.png")
        cavalry (q/load-image "resources/sprites/cavalry.png")
        artillery (q/load-image "resources/sprites/artillery.png")
        field (q/load-image "resources/sprites/field.png")
        trees (q/load-image "resources/sprites/trees.png")
        mountains (q/load-image "resources/sprites/mountains.png")]
    (load-images [infantry cavalry artillery])
    (resize-images [infantry cavalry artillery] unit-size unit-size)
    (load-images [field trees])
    (resize-images [field trees mountains] tile-size tile-size)
    {:infantry infantry
     :cavalry cavalry
     :artillery artillery
     :field field
     :mountains mountains
     :trees trees}))

(defn add-sprite [unit images]
  (if (images (:unit-type unit))
    (assoc unit :sprite ((:unit-type unit) images))
    unit))

(defn add-sprites-to-units [game-state]
  (-> game-state
      (update-in [:red :units] update-vals add-sprite (:images game-state))
      (update-in [:blue :units] update-vals add-sprite (:images game-state))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Drawing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-tile [x y color]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill color)
  (q/rect x y tile-size tile-size))

;; terrain
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-road [x y dirs]
  (let [s20 (scale 20)
        s40 (scale 40)
        s60 (scale 60)
        s100 (scale 100)]
    (doseq [d dirs]
      (q/stroke 0)
      (q/stroke-weight 0)
      (q/fill (get-in colors [:terrain :road]))
      (case d
        :hor (q/rect x (+ y s40) s100 s20)
        :vert (q/rect (+ x s40) y s20 s100)
        :ne (q/quad (+ x s40) y
                    (+ x s60) y
                    (+ x s100) (+ y s40)
                    (+ x s100) (+ y s60))
        :nw (q/quad (+ x s40) y
                    (+ x s60) y
                    x (+ y s60)
                    x (+ y s40))
        :se (q/quad (+ x s40) (+ y s100)
                    (+ x s60) (+ y s100)
                    (+ x s100) (+ y s60)
                    (+ x s100) (+ y s40))
        :sw (q/quad (+ x s40) (+ y s100)
                    (+ x s60) (+ y s100)
                    x (+ y s40)
                    x (+ y s60))))))

(defn draw-sprite [x y sprite]
  (q/image-mode :corner)
  (q/resize sprite tile-size tile-size)
  (q/image sprite x y))

(defn draw-terrain [tiles images]
  (doseq [tile tiles]
    (let [[x y] (map coord->px (:grid tile))]
      (case (:terrain tile)
        :field (draw-sprite x y (:field images))
        :trees (draw-sprite x y (:trees images))
        :mountains (draw-sprite x y (:mountains images))
        :road (draw-road x y (:dirs tile))
        nil))))

;; units
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-unit [{:keys [position id hp sprite]} color]
  (let [x (coord->px (first position))
        y (coord->px (second position))]
    (draw-tile x y color)
    (q/stroke 0)
    (q/stroke-weight 0)
    (q/fill (colors :white))
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (q/text (name id) (+ x (scale 15)) (+ y (scale 30)))
    (q/text-font (q/create-font "Courier New" (scale 20)))
    (q/text (str hp) (+ x (scale 70)) (+ y (scale 90)))
    (when sprite
      (q/image-mode :corner)
      (q/image sprite x (+ y (scale 35))))))

(defn draw-shadow-unit [unit]
  (draw-unit unit (get-in colors [(:side unit) :shadow])))

(defn draw-units [game-state side]
  (doseq [unit (vals (get-in game-state [side :units]))]
    (let [color (cond
                  (zero? (:move-points unit)) (get-in colors [side :spent])
                  :else (get-in colors [side :default]))]
      (draw-unit unit color))))

;; other on-map
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn draw-attack-cursor [[x y]]
  (q/fill nil)
  (q/stroke (colors :attack-cursor))
  (q/stroke-weight (scale 12))
  (q/ellipse (+ (coord->px x) (scale 50))
             (+ (coord->px y) (scale 50))
             (scale 70) (scale 70)))

;; menus
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-turn-indicator [side]
  (q/stroke nil)
  (q/fill (vec (take 3 (get-in colors [side :default]))))
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

(defn debug-text-item [line-num text]
  (q/text text
          (+ (scale 25) (scale 3))
          (- (+ (* line-num (scale 30)) (scale 50)) (scale 3))))

(defn draw-debug-box [game-state]
  (let [{:keys [cursor selected
                unit-under-cursor uuc-defence
                unit-selected selected-defence
                route-selection route route-cost
                attack-option]} (game/debug-data game-state)]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight (scale 6))
    (q/rect (scale 3) (scale 3) (scale 1000) (scale 300))
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (debug-text-item 0 (str "Cursor: " cursor " Selected: " selected))

    (when unit-under-cursor
      (debug-text-item 1 (str "CURSOR: " (:hp unit-under-cursor) "hp"))
      (debug-text-item 2 (str "Attack option: " attack-option))
      (debug-text-item 3 (str "Move points: " (:move-points unit-under-cursor)))
      (debug-text-item 4 (str "Att/Def: " (:attack unit-under-cursor) "/" uuc-defence)))
    (when (and selected (not unit-under-cursor))
      (debug-text-item 1 (str "SELECTED: " (:hp unit-selected) "hp"))
      (debug-text-item 2 (str "Attack option: " attack-option))
      (debug-text-item 3 (str "Move points: " (:move-points unit-selected)))
      (debug-text-item 4 (str "Att/Def: " (:attack unit-selected) "/" selected-defence)))
    (when route-selection
      (debug-text-item 5 (str "Route: " route))
      (debug-text-item 6 (str "Route cost: " route-cost)))))

(defn draw-state [game-state]
  (q/background 240)
  (draw-terrain (vals (:field game-state)) (:images game-state))
  (when (:route-selection game-state) (draw-routing (:route game-state)))
  (draw-units game-state :red)
  (draw-units game-state :blue)
  (when (:shadow-unit game-state) (draw-shadow-unit (:shadow-unit game-state)))
  (when (:highlight game-state) (draw-highlights (:highlight game-state)))
  (if (:attack-mode game-state)
    (draw-attack-cursor (:cursor game-state))
    (draw-cursor (:cursor game-state)))
  (when (:menu game-state) (draw-menu game-state))
  (when (:debug game-state) (draw-debug-box game-state))
  (draw-turn-indicator (:turn game-state)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Setup and run
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn setup []
  (q/frame-rate fps)
  (let [ss (-> game-state
               (assoc :cursor [(int (/ horiz-tiles 2)) (int (/ vert-tiles 2))]
                      :images (load-sprites))
               add-sprites-to-units)]
    ss))

(q/defsketch game
  :host "map"
  :size [(* horiz-tiles tile-size) (* vert-tiles tile-size)]
  :setup setup
  :settings #(q/smooth 2)
  :draw draw-state
  :update #(do (reset! debug %) (tick %))
  :key-pressed key-handler
  :middleware [m/fun-mode])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; debug
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  @debug
  (:shadow-unit @debug)
  (game/debug-data @debug)

  (select-keys @debug [:dispatch :order-queue]))