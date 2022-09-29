(ns general-slim.ui
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.ui-layers :as layers]
            [general-slim.utils :refer [update-vals coord+]]
            [general-slim.game :as game :refer [tick key-handler]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; state and constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def debug (atom {}))
(def game-state game/game-state)
(def fps 30)
(def horiz-tiles game/horiz-tiles)
(def vert-tiles game/vert-tiles)
(def tile-size game/tile-size)
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
        road-hor (q/load-image "resources/sprites/road_hor.png")
        road-vert (q/load-image "resources/sprites/road_vert.png")
        road-ul (q/load-image "resources/sprites/road_ul.png")
        road-ur (q/load-image "resources/sprites/road_ur.png")
        road-dl (q/load-image "resources/sprites/road_dl.png")
        road-dr (q/load-image "resources/sprites/road_dr.png")]
    (load-images [red-inf blue-inf field trees mountains road-hor road-vert road-ul road-ur road-dl road-dr])
    (resize-images [red-inf blue-inf field trees mountains road-hor road-vert road-ul road-ur road-dl road-dr] tile-size tile-size)
    {:infantry {:red red-inf :blue blue-inf}
     :field field
     :mountains mountains
     :trees trees
     :roads {:hor road-hor
             :vert road-vert
             :ul road-ul
             :ur road-ur
             :dl road-dl
             :dr road-dr}}))

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
  (q/image sprite x y))

(defn draw-road [[x y] dirs images]
  (q/image-mode :corner)
  (q/image (:field images) x y)
  (doseq [dir dirs]
    (q/image (get-in images [:roads dir]) x y)))

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

(defn draw-unit-hover-menu [unit _camera]
  (when unit
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
      (q/fill (colors :menu-select)))))

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

(defn draw-units [units camera]
  (doseq [unit units]
    (let [side (:side unit)
          color (cond
                  (zero? (:move-points unit)) (get-in colors [side :spent])
                  (:move-over unit) (get-in colors [side :spent])
                  :else (get-in colors [side :default]))]
      (draw-unit unit camera color))))

(defn draw-viewsheds [viewsheds camera]
  (doseq [[x y] viewsheds]
    (q/stroke 0)
    (q/stroke-weight 0)
    (draw-tile (camera-offset [x y] camera)
               (:fow colors))))

;; other on-map
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-attack-cursor [coord camera]
  (let [[x y] (coord+ (camera-offset coord camera) [(scale 50) (scale 50)])]
    (q/fill nil)
    (q/stroke (colors :attack-cursor))
    (q/stroke-weight (scale 12))
    (q/ellipse x y
               (scale 70) (scale 70))))

(defn draw-cursor [{:keys [type cursor]} camera]
  (if (= type :attack)
    (draw-attack-cursor cursor camera)
    (draw-tile (camera-offset cursor camera) (colors :cursor))))

(defn draw-highlights [coords camera]
  (when coords
    (doseq [coord coords]
      (draw-tile (camera-offset coord camera)
                 (colors :map-highlight)))))

(defn draw-routing [coords camera]
  (when coords
    (doseq [coord coords]
      (draw-tile (camera-offset coord camera)
                 (colors :routing)))))

;; menus
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-turn-indicator [[side turn-number] _camera]
  (q/stroke nil)
  (q/fill (vec (take 3 (get-in colors [side :default]))))
  (q/rect 0 0 (scale 160) (scale 60))
  (q/fill (colors :white))
  (q/text-font (q/create-font "Courier New" (scale 30)))
  (q/text (str "Turn: " turn-number) (scale 10) (scale 40)))

(defn draw-menu [menu _camera]
  (when menu
    (let [x-offset (scale 50) y-offset (scale 50)
          line-offset (scale 35)]
      (q/stroke 1)
      (q/fill (colors :white))
      (q/stroke-weight (scale 6))
      (q/rect x-offset y-offset (scale 300) (scale 300))
      (q/fill 0)
      (q/text-font (q/create-font "Courier New" (scale 30)))
      (doseq [[row item] (map-indexed vector (vals (get-in menu [:options])))]
        (q/text item (+ (scale 20) x-offset) (+ (* row line-offset) (scale 40) y-offset)))
      (q/stroke-weight 1)
      (q/fill (colors :menu-select))
      (q/rect (+ x-offset (scale 10)) (+ (* line-offset (get-in menu [:selection])) (+ y-offset (scale 10))) (scale 250) (scale 35)))))

(defn debug-text-item [line-num text]
  (q/text text
          (+ (scale 25) (scale 3))
          (- (+ (* line-num (scale 30)) (scale 50)) (scale 3))))

(keys @debug)

(defn draw-debug-box [debug-data _camera]
  (when debug-data
    (let [{:keys [cursor selected
                  unit-under-cursor
                  unit-selected
                  route-selection route route-cost
                  attack-option camera
                  ticks]} debug-data]
      (q/stroke 1)
      (q/fill (colors :white))
      (q/stroke-weight (scale 6))
      (q/rect (scale 3) (scale 3) (scale 1000) (scale 300))
      (q/fill 0)
      (q/text-font (q/create-font "Courier New" (scale 30)))
      (debug-text-item 0 (str "            Tick: " ticks " Cursor: " cursor " Selected: " selected " Camera: " camera))

      (when unit-under-cursor
        (debug-text-item 1 (str "CURSOR: " (:id unit-under-cursor)))
        (debug-text-item 2 (str (:soldiers unit-under-cursor) " soldiers"))
        (debug-text-item 3 (str "Attack option: " attack-option))
        (debug-text-item 4 (str "Move points: " (:move-points unit-under-cursor)))
        (debug-text-item 5 (str "Intel: " (mapv (juxt :position :side :sight-time :is-current) (vals (:intel unit-under-cursor))))))
      (when (and selected (not unit-under-cursor))
        (debug-text-item 1 (str "SELECTED: " (:id unit-selected)))
        (debug-text-item 2 (str (:soldiers unit-selected) " soldiers"))
        (debug-text-item 3 (str "Attack option: " attack-option))
        (debug-text-item 4 (str "Move points: " (:move-points unit-selected))))
      (when route-selection
        (debug-text-item 5 (str "Route: " route))
        (debug-text-item 6 (str "Route cost: " route-cost))))))

(defn draw-state [game-state]
  (let [{:keys [images camera]} (layers/constants game-state)]
    (draw-terrain (layers/field-layer game-state) images camera)
    (draw-units (layers/unit-layer game-state) camera)
    (draw-highlights (layers/highlight-layer game-state) camera)
    (draw-routing (layers/route-layer game-state) camera)
    (draw-viewsheds (layers/viewsheds-layer game-state) camera)
    (draw-cursor (layers/cursor-layer game-state) camera)
    (draw-unit-hover-menu (layers/hover-info-layer game-state) camera)
    (draw-menu (layers/menu-layer game-state) camera)
    (draw-debug-box (layers/debug-layer game-state) camera)
    (draw-turn-indicator (layers/turn-indicator-layer game-state) camera)))

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

(defn go []
  (q/defsketch game
    :host "map"
    :size [screen-size-x screen-size-y]
    :setup setup
    :settings #(q/smooth 2)
    :draw draw-state
    :update #(do (reset! debug %) (tick %))
    :key-pressed key-handler
    :middleware [m/fun-mode]))

(comment
  (q/defsketch game
    :host "map"
    :size [screen-size-x screen-size-y]
    :setup setup
    :settings #(q/smooth 2)
    :draw draw-state
    :update #(do (reset! debug %) (tick %))
    :key-pressed key-handler
    :middleware [m/fun-mode]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; debug
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  @debug
  (:shadow-unit @debug)
  (game/debug-data @debug)

  (select-keys @debug [:attack-mode])
  (dissoc @debug :field))
