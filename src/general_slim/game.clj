(ns general-slim.game
  (:require [general-slim.field :as field]
            [general-slim.forces :as forces]
            [general-slim.inputs :as inputs]))

(def game-state {:field (field/flat-field 10 10)
                 :red forces/red
                 :blue forces/blue
                 :turn :red
                 :turn-number 0})

(defn tick [game-state]
  (if-let [order (:order game-state)]
    (inputs/handle-input game-state order)
    (update game-state :turn-number inc)))

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