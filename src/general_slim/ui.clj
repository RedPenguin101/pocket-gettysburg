(ns general-slim.ui
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [general-slim.main :refer [tick]]
            [general-slim.forces :refer [unit-in-square]]))


(def debug (atom {}))
(def grid-size 10)
(def cell-size (quot 1000 grid-size))
(def colors {:cursor [183 183 183]
             :map-highlight [220 220 220]
             :red {:default [211 61 61]
                   :spent [150 42 42]
                   :selected [252 126 126]}
             :blue {:default [61 106 211]
                    :spent [150 42 42]
                    :selected [106 149 252]}})

(defn adjacents [[x y]]
  #{[(inc x) y] [(dec x) y]
    [x (inc y)] [x (dec y)]})

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
  (assoc general-slim.main/game-state :cursor [5 5]))

(defn draw-tile [x y color]
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill color)
  (q/rect x y cell-size cell-size))

(defn draw-unit [{:keys [position]} color]
  (draw-tile (coord->px (first position))
             (coord->px (second position))
             color))

(defn draw-units [game-state side]
  (doseq [unit (vals (get-in game-state [side :units]))]
    (let [color (if (= (:position unit) (:selected game-state))
                  (get-in colors [side :selected])
                  (get-in colors [side :default]))]
      (draw-unit unit color))))

(defn draw-cursor [[x y]]
  (draw-tile (coord->px x) (coord->px y) (colors :cursor)))

(defn draw-highlights [coords]
  (doseq [[x y] coords]
    (draw-tile (coord->px x) (coord->px y)
               (colors :map-highlight))))

(defn draw-state [game-state]
  (q/background 240)
  (when (:highlight game-state) (draw-highlights (:highlight game-state)))
  (draw-units game-state :red)
  (draw-units game-state :blue)
  (draw-cursor (:cursor game-state)))

(defn handle-selection [game-state]
  (let [cursor (:cursor game-state)
        unit-under-cursor? (unit-in-square game-state cursor)
        side (:turn game-state)
        selected? (:selected game-state)
        selected-unit? (unit-in-square game-state selected?)]
    (cond
      ;; If no selection, and trying to select your unit, select
      (and (not selected?) (= side (:side unit-under-cursor?)))
      (assoc game-state :selected cursor :highlight (adjacents cursor))
      ;; If there's a selected unit, move it
      selected-unit? (dissoc (assoc game-state :order [:move side (:id selected-unit?) cursor]) :selected :highlight)
      :else (do (println "Selection fall through") game-state))))

(defn key-handler [game-state event]
  (case (:key event)
    :up (update game-state :cursor (comp bound up))
    :down (update game-state :cursor (comp bound down))
    :left (update game-state :cursor (comp bound left))
    :right (update game-state :cursor (comp bound right))
    :space (handle-selection game-state)
    :c (assoc game-state :order [:end-turn (:turn game-state)])
    game-state))

(comment
  (q/defsketch game
    :host "map"
    :size [1000 1000]
    :setup setup
    :settings #(q/smooth 2)
    :draw draw-state
    :update #(do (reset! debug %) (tick %))
    :key-pressed key-handler
    :middleware [m/fun-mode]))

(dissoc @debug :field)