(ns general-slim.game
  (:require [general-slim.forces :as forces]
            [general-slim.inputs :as inputs :refer [route-cost]]
            [general-slim.dispatches :as d]
            [general-slim.viewsheds :as vs]
            [general-slim.field :as field]
            [general-slim.utils :refer [relative-coord]]
            [general-slim.scenario-loader :refer [load-scenario]]))

;; state and constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def game-state
  (-> (load-scenario "aw_ft1")
      (assoc :camera [0 0])
      (vs/add-viewshed-to-units :red)
      (vs/add-viewshed-to-units :blue)))

(let [[x y] (:field-size game-state)]
  (def horiz-tiles x)
  (def vert-tiles y))

(def tile-size (* 2 32))
(def colors {:cursor [183 183 183 75]
             :attack-cursor [215 221 33 90]
             :map-highlight [220 220 220 75]
             :routing [101 252 90 75]
             :terrain {:trees [36 119 23]
                       :mountains [124 117 104]
                       :road [140 101 33]}
             :red {:default [211 61 61 150]
                   :shadow [211 61 61 75]
                   :spent [150 42 42 150]
                   :selected [252 126 126 150]}
             :blue {:default [61 106 211 150]
                    :shadow [61 106 211 75]
                    :spent [37 68 142 150]
                    :selected [106 149 252 150]}
             :white [252 252 252]
             :menu-select [183 183 183 75]
             :fow [0 0 0 50]})

;; Menu definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatch-menu [dispatch attack-option]
  {:name :dispatch-menu
   :message (d/print-dispatch dispatch)
   :options (if (= :no-targets attack-option)
              {:send-order "> Send Order"}
              {:attack "> Attack"
               :send-order "> Send Order"})
   :selection 0})

;; utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bound [[x y]]
  [(min (max 0 x) (dec horiz-tiles))
   (min (max 0 y) (dec vert-tiles))])

(defn new-camera [[c-x c-y] [x y]]
  [(cond (< x c-x) (dec c-x)
         (> x (+ c-x 14)) (inc c-x)
         :else c-x)
   (cond (< y c-y) (dec c-y)
         (> y (+ c-y 14)) (inc c-y)
         :else c-y)])

;; Input handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Cursors

(defn add-unit-under-cursor [game-state new-cursor]
  (if-let [uuc (forces/unit-at-location game-state new-cursor)]
    (assoc game-state :unit-under-cursor uuc)
    (dissoc game-state :unit-under-cursor)))

(defn cursor-map [game-state new-cursor]
  (-> game-state
      (assoc :cursor new-cursor)
      (add-unit-under-cursor new-cursor)
      (update :camera new-camera new-cursor)))

(defn cursor-with-selection [game-state new-cursor]
  (let [selected-unit (forces/unit-at-location game-state (:selected game-state))]
    (cond (= new-cursor (:cursor game-state))
          game-state

          ;; can't move through units
          (and (forces/unit-at-location game-state new-cursor)
               (not= new-cursor (:position selected-unit)))
          game-state

          ;; can always back out a selection
          (= new-cursor (second (:route game-state)))
          (-> game-state
              (assoc :cursor new-cursor)
              (update :route rest))

          ;; if route cost of the new route is lte move points of the unit
          ;; the the new route is OK
          (<= (route-cost game-state selected-unit (reverse (conj (:route game-state) new-cursor)))
              (:move-points selected-unit))
          (-> game-state
              (assoc :cursor new-cursor)
              (update :route conj new-cursor))

          :else game-state)))

(defn cursor-attack [game-state dir]
  (let [[_side _unit-id unit-loc target-locs] (:attack-mode game-state)
        attempted-selection (#(relative-coord % dir) unit-loc)]
    (if (target-locs attempted-selection)
      (assoc game-state :cursor attempted-selection)
      game-state)))

(defn cursor-menu [game-state dir]
  (let [menu-items (count (get-in game-state [:menu :options]))]
    (case dir
      :down (update-in game-state [:menu :selection] #(mod (inc %) menu-items))
      :up (update-in game-state [:menu :selection] #(mod (dec %) menu-items))
      game-state)))

(defn handle-cursor [game-state dir]
  (let [new-cursor (bound (relative-coord (:cursor game-state) dir))]
    (cond (:menu game-state)        (cursor-menu game-state dir)
          (:attack-mode game-state) (dissoc (cursor-attack game-state dir) :unit-under-cursor)
          (:selected game-state)    (dissoc (cursor-with-selection game-state new-cursor) :unit-under-cursor)
          :else                     (cursor-map game-state new-cursor))))

;; Action button

(defn action-select [game-state]
  (let [cursor (:cursor game-state)
        unit-under-cursor? (forces/unit-at-location game-state cursor)
        my-side (:turn game-state)]
    (cond
      (not unit-under-cursor?)
      (do (println "No unit to select") game-state)

      (:move-over unit-under-cursor?)
      (do (println "Unit is spent") game-state)

      ;; trying to select your unit, select and turn on route selection
      (and (= my-side (:side unit-under-cursor?))
           (forces/can-move? unit-under-cursor?))
      (assoc game-state
             :route-selection true
             :route (list cursor)
             :selected cursor
             :dispatch (d/start-dispatch my-side (:id unit-under-cursor?))
             :highlight (inputs/can-move-to game-state unit-under-cursor?))

      ;; selecting the other sides unit, show the movement range
      (and (not= my-side (:side unit-under-cursor?))
           (forces/can-move? unit-under-cursor?))
      (assoc game-state :highlight (inputs/can-move-to game-state unit-under-cursor?))

      :else (do (println "Selection fall through") game-state))))

(defn request-input [game-state]
  (assoc game-state :menu (dispatch-menu (:dispatch game-state) (:attack-option game-state))))

(defn action-move-plan [game-state]
  (let [cursor (:cursor game-state)
        unit-under-cursor? (forces/unit-at-location game-state cursor)
        my-side (:turn game-state)
        selected? (:selected game-state)
        selected-unit? (forces/unit-at-location game-state selected?)]

    (when (and selected-unit? (not= unit-under-cursor? selected-unit?))
      (println "Main move plan branch"))

    (cond
      ;; If there's a selected unit, update the dispatch, draw the shadown
      ;; unit at the selected location, and add attack options if there are
      ;; any. Pop the dispatch menu
      (and selected-unit? (not= unit-under-cursor? selected-unit?))
      (-> game-state
          (update :dispatch d/add-move-order (reverse (butlast (:route game-state))))
          (assoc :shadow-unit (assoc selected-unit? :position cursor))
          (inputs/add-attack-option my-side (:id selected-unit?) cursor)
          (request-input)
          (dissoc :highlight :route-selection :route))

      ;; special case, no move
      selected-unit?
      (-> game-state
          (inputs/add-attack-option my-side (:id selected-unit?) cursor)
          (request-input)
          (dissoc :highlight :route-selection :route))

      :else (do (println "Move fall through") game-state))))

(defn action-menu [game-state]
  (let [selected-option (nth (keys (get-in game-state [:menu :options]))
                             (get-in game-state [:menu :selection]))]
    (case selected-option
      :send-order
      (-> game-state
          (d/send-order)
          (dissoc :shadow-unit :selected :highlight :attack-option :menu))
      :attack
      (-> game-state
          (assoc :cursor (first (last (:attack-option game-state)))
                 :attack-mode (:attack-option game-state))
          (dissoc :menu :attack-option))

      (do (println "Menu Fallthrough") game-state))))

(defn action-attack [game-state]
  (let [defender-id (:id (forces/unit-at-location game-state (:cursor game-state)))]
    (-> game-state
        (update :dispatch d/add-attack-order defender-id)
        (d/send-order)
        (dissoc :attack-mode :selected :shadow-unit))))

(defn handle-action [game-state]
  (cond (:menu game-state)        (action-menu game-state)
        (:attack-mode game-state) (action-attack game-state)
        (:selected game-state)    (action-move-plan game-state)
        :else                     (action-select game-state)))

(defn handle-end-turn [game-state]
  (if (:menu game-state)
    game-state
    (-> game-state
        (assoc :order-queue [[:end-turn (:turn game-state)]])
        (dissoc :selected :highlight :shadow-unit))))

(defn handle-cancel [game-state]
  (dissoc game-state
          :route-selection :route :selected :highlight
          :menu :shadow-unit :dispatch
          :attack-mode :attack-option))

(defn key-handler [game-state event]
  (case (:key event)
    :up    (handle-cursor game-state :up)
    :w     (handle-cursor game-state :up)
    :down  (handle-cursor game-state :down)
    :s     (handle-cursor game-state :down)
    :left  (handle-cursor game-state :left)
    :a     (handle-cursor game-state :left)
    :right (handle-cursor game-state :right)
    :d     (handle-cursor game-state :right)

    :space (handle-action game-state)

    :g     (update game-state :debug not)
    :e     (handle-end-turn game-state)
    :q     (handle-cancel game-state)
    game-state))

;; debug

(defn debug-data [game-state]
  (let [cursor (:cursor game-state)
        selected (:selected game-state)
        uuc (forces/unit-at-location game-state cursor)
        su (forces/unit-at-location game-state selected)]
    {:camera (:camera game-state)
     :cursor cursor
     :selected selected
     :unit-under-cursor uuc
     :uuc-defence (when uuc (forces/defence-value uuc (get-in game-state [:field (:position uuc) :terrain])))
     :unit-selected su
     :selected-defence (when su (forces/defence-value su (get-in game-state [:field (:position su) :terrain])))
     :route-selection (:route-selection game-state)
     :route (:route game-state)
     :route-cost (when (:route game-state) (route-cost game-state su (reverse (:route game-state))))
     :attack-option (:attack-option game-state)}))

;; Top lvl tick

(defn tick [game-state]
  (if (or (:current-order game-state) (not-empty (:order-queue game-state)))
    (update (inputs/handle-input game-state) :ticks (fnil inc 0))
    (update game-state :ticks (fnil inc 0))))

(defn main-loop [state]
  (if (> (:turn-number state) 10)
    state
    (recur (tick state))))

(comment
  "Debug area"
  (def other-side {:red :blue :blue :red})

  (def game-state @general-slim.ui/debug)
  @general-slim.ui/debug

  (require '[general-slim.field :as field])
  (require '[general-slim.utils :as utils])
  (field/terrain-map (:field game-state) (utils/manhattan [4 4] 4))
  (field/terrain-map (:field game-state))

  game-state
  (:red game-state)
  (tick game-state)
  (select-keys game-state [:dispatch :menu :attack-option]))
