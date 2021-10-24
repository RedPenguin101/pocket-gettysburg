(ns general-slim.game
  (:require [general-slim.forces :refer [can-move? unit-in-square defence-value]]
            [general-slim.inputs :as inputs :refer [can-move-to route-cost]]
            [general-slim.game-states :as gs]))

;; state and constants

(def game-state gs/aw-ft1)
(def fps 30)
(let [[x y] (:field-size game-state)]
  (def horiz-tiles x)
  (def vert-tiles y))
(def tile-size 75)
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

;; Menus

(def attack-menu {:name :attack-menu
                  :options {:attack "> Attack"
                            :wait "> Wait"}
                  :selection 0})

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

;; debug

(defn debug-data [game-state]
  (let [cursor (:cursor game-state)
        selected (:selected game-state)
        uuc (unit-in-square game-state cursor)
        su (unit-in-square game-state selected)]
    {:cursor cursor
     :selected selected
     :unit-under-cursor uuc
     :uuc-defence (when uuc (defence-value uuc (get-in game-state [:field (:position uuc) :terrain])))
     :unit-selected su
     :selected-defence (when su (defence-value su (get-in game-state [:field (:position su) :terrain])))
     :route-selection (:route-selection game-state)
     :route (:route game-state)
     :route-cost (when (:route game-state) (route-cost game-state su (reverse (:route game-state))))
     :attack-option (:attack-option game-state)}))

(debug-data @general-slim.ui/debug)

;; Top lvl tick

(defn tick [game-state]
  (cond (:order game-state)
        (inputs/handle-input game-state (:order game-state))

        (and (not (:menu game-state)) (:attack-option game-state))
        (assoc game-state :menu attack-menu)

        :else game-state))

(defn main-loop [state]
  (if (> (:turn-number state) 10)
    state
    (recur (-> state
               tick))))

(comment
  ;; basic move
  (dissoc (main-loop game-state) :field)
  ;; end turn
  (dissoc (main-loop (assoc game-state :order [:end-turn :red])) :field)
  ;; can't end turn if it's not your turn
  (dissoc (main-loop (assoc game-state :order [:end-turn :blue])) :field))