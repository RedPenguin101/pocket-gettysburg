(ns general-slim.main
  (:require [general-slim.field :as field]
            [general-slim.forces :as forces]
            [general-slim.inputs :as inputs]))

(def game-state {:field (field/flat-field 10 10)
                 :red forces/side2
                 :blue forces/side1
                 :turn :red
                 :turn-number 0})

(general-slim.inputs/handle-input
 game-state
 [:red :inf1 :move [6 7]])

(defn tick [game-state]
  (update game-state :turn-number inc))

(defn display [state]
  state)

(defn main-loop [state]
  (if (> (:turn-number state) 10)
    state
    (recur (-> state
               tick
               display))))

(comment
  (main-loop game-state))