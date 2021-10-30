(ns general-slim.ui
  (:require [clojure.set :as set]
            [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.field :as field]
            [general-slim.forces :as forces]
            [general-slim.utils :refer [update-vals coord+]]
            [general-slim.game :as game :refer [tick key-handler]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; state and constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def debug (atom {}))
(def game-state game/game-state)
(def fps game/fps)
(def horiz-tiles game/horiz-tiles)
(def vert-tiles game/vert-tiles)
(def tile-size game/tile-size)
(def unit-size tile-size)
(def colors game/colors)
(def scale-factor (/ tile-size 100))
(def screen-size-x (* (min 15 horiz-tiles) tile-size))
(def screen-size-y (* (min 15 vert-tiles) tile-size))

(defn scale [x] (int (* scale-factor x)))

(defn camera-offset [[x y] [c-x c-y]]
  [(int (* tile-size (- x c-x)))
   (int (* tile-size (- y c-y)))])

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
  (let [red-inf (q/load-image "resources/sprites/red_inf.png")
        blue-inf (q/load-image "resources/sprites/blue_inf.png")
        field (q/load-image "resources/sprites/field2.png")
        trees (q/load-image "resources/sprites/trees2.png")
        mountains (q/load-image "resources/sprites/mountains2.png")
        road-straight (q/load-image "resources/sprites/road_straight.png")
        road-corner (q/load-image "resources/sprites/road_corner.png")]
    (load-images [red-inf blue-inf field trees mountains road-straight road-corner])
    (resize-images [red-inf blue-inf] unit-size unit-size)
    (resize-images [field trees mountains road-straight] tile-size tile-size)
    {:infantry {:red red-inf
                :blue blue-inf}
     :field field
     :mountains mountains
     :trees trees
     :road-straight road-straight
     :road-corner road-corner}))

(defn add-sprite [unit images]
  (if (images (:unit-type unit))
    (assoc unit :sprite (get-in images [(:unit-type unit) (:side unit)]))
    unit))

(defn add-sprites-to-units [game-state]
  (-> game-state
      (update-in [:red :units] update-vals add-sprite (:images game-state))
      (update-in [:blue :units] update-vals add-sprite (:images game-state))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Drawing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-tile [[x y] color]
  (q/stroke nil)
  (q/fill color)
  (q/rect x y tile-size tile-size))

;; terrain
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-sprite [[x y] sprite]
  (q/image-mode :corner)
  (q/resize sprite tile-size tile-size)
  (q/image sprite x y))

(defn draw-road [[x y] dirs images]
  (q/image-mode :corner)
  (q/image (:field images) x y)
  (when ((set dirs) :hor) (q/image (:road-straight images) x y)))

(defn draw-terrain [tiles images camera]
  (doseq [tile tiles]
    (let [coord (camera-offset (:grid tile) camera)]
      (case (:terrain tile)
        :field (draw-sprite coord (:field images))
        :trees (draw-sprite coord (:trees images))
        :mountains (draw-sprite coord (:mountains images))
        :road (draw-road coord (:dirs tile) images)
        nil))))

;; units
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-unit-hover-menu [unit]
  (let [[x _y] (:position unit)
        x-offset (+ (if (>= x (/ horiz-tiles 2))
                      (scale 50)
                      (- screen-size-x (scale 550))))
        y-offset (scale 50)
        line-offset (scale 35)]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight (scale 6))
    (q/rect x-offset y-offset (scale 500) (scale 300))
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (q/text (:unit-name unit) (+ (scale 20) x-offset) (+ (* 0 line-offset) (scale 40) y-offset))
    (q/text (str "Soldiers: " (:soldiers unit)) (+ (scale 20) x-offset) (+ (* 1 line-offset) (scale 40) y-offset))
    (q/stroke-weight 1)
    (q/fill (colors :menu-select))))

(defn draw-unit [{:keys [position short-name sprite]} camera color]
  (let [[x y] (camera-offset position camera)]
    (draw-tile [x y] color)
    (q/stroke 0)
    (q/stroke-weight 0)
    (q/fill (colors :white))
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (q/text short-name (+ x (scale 15)) (+ y (scale 30)))
    (when sprite
      (q/image-mode :corner)
      (q/image sprite x (+ y (scale 15))))))

(defn filter-units-by-position-set [units pos-set]
  (filter (fn [u] (pos-set (:position u))) units))

(defn draw-units [game-state side]
  (let [units (vals (get-in game-state [side :units]))
        units (if (and (:viewsheds game-state) (not= (:turn game-state) side)) (filter-units-by-position-set units (:viewsheds game-state)) units)
        units (filter #(forces/unit-in-view? % (:camera game-state)) units)]
    (doseq [unit units]
      (let [color (cond
                    (zero? (:move-points unit)) (get-in colors [side :spent])
                    (:move-over unit) (get-in colors [side :spent])
                    :else (get-in colors [side :default]))]
        (draw-unit unit (:camera game-state) color))))
  (doseq [[x y] (set/difference (set (keys (:field game-state))) (:viewsheds game-state))]
    (q/stroke 0)
    (q/stroke-weight 0)
    (draw-tile (camera-offset [x y] (:camera game-state))
               (:fow colors))))

;; other on-map
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-cursor [cursor camera]
  (draw-tile (camera-offset cursor camera) (colors :cursor)))

(defn draw-highlights [coords camera]
  (doseq [coord coords]
    (draw-tile (camera-offset coord camera)
               (colors :map-highlight))))

(defn draw-routing [coords camera]
  (doseq [coord coords]
    (draw-tile (camera-offset coord camera)
               (colors :routing))))

(defn draw-attack-cursor [coord camera]
  (let [[x y] (coord+ (camera-offset coord camera) [(scale 50) (scale 50)])]
    (q/fill nil)
    (q/stroke (colors :attack-cursor))
    (q/stroke-weight (scale 12))
    (q/ellipse x y
               (scale 70) (scale 70))))

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
                unit-under-cursor
                unit-selected
                route-selection route route-cost
                attack-option camera]} (game/debug-data game-state)]
    (q/stroke 1)
    (q/fill (colors :white))
    (q/stroke-weight (scale 6))
    (q/rect (scale 3) (scale 3) (scale 1000) (scale 300))
    (q/fill 0)
    (q/text-font (q/create-font "Courier New" (scale 30)))
    (debug-text-item 0 (str "Cursor: " cursor " Selected: " selected " Camera: " camera))

    (when unit-under-cursor
      (debug-text-item 1 (str "CURSOR: " (:id unit-under-cursor)))
      (debug-text-item 2 (str (:soldiers unit-under-cursor) " soldiers"))
      (debug-text-item 3 (str "Attack option: " attack-option))
      (debug-text-item 4 (str "Move points: " (:move-points unit-under-cursor))))
    (when (and selected (not unit-under-cursor))
      (debug-text-item 1 (str "SELECTED: " (:id unit-selected)))
      (debug-text-item 2 (str (:soldiers unit-selected) " soldiers"))
      (debug-text-item 3 (str "Attack option: " attack-option))
      (debug-text-item 4 (str "Move points: " (:move-points unit-selected))))
    (when route-selection
      (debug-text-item 5 (str "Route: " route))
      (debug-text-item 6 (str "Route cost: " route-cost)))))

(defn draw-state [game-state]
  (draw-terrain (vals (field/terrain-in-view (:field game-state) (:camera game-state)))
                (:images game-state)
                (:camera game-state))
  (when (:route-selection game-state) (draw-routing (:route game-state) (:camera game-state)))
  (draw-units game-state :red)
  (draw-units game-state :blue)
  (when (:highlight game-state) (draw-highlights (:highlight game-state) (:camera game-state)))
  (if (:attack-mode game-state)
    (draw-attack-cursor (:cursor game-state) (:camera game-state))
    (draw-cursor (:cursor game-state) (:camera game-state)))
  (when (and (:unit-under-cursor game-state)
             ((:viewsheds game-state) (:cursor game-state)))
    (draw-unit-hover-menu (:unit-under-cursor game-state)))
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
  :size [screen-size-x screen-size-y]
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

  (select-keys @debug [:attack-mode])
  (dissoc @debug :field))
