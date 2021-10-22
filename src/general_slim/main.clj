(ns general-slim.main
  (:require [general-slim.field :as field]
            [general-slim.forces :as forces]
            [general-slim.inputs :as inputs]))

(def game-state {:field (field/flat-field 10 10)
                 :red forces/side2
                 :blue forces/side1
                 :turn :red
                 :turn-number 0
                 :order [:red :inf1 :move [6 7]]})

(defn tick [game-state]
  (if-let [order (:order game-state)]
    (inputs/handle-input (dissoc game-state :order) order)
    (update game-state :turn-number inc)))

(defn interface [state]
  state)

(defn main-loop [state]
  (if (> (:turn-number state) 10)
    state
    (recur (-> state
               tick
               interface))))

(comment
  (dissoc (main-loop game-state) :field))