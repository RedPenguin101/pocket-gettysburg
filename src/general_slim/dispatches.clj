(ns general-slim.dispatches
  (:require [clojure.string :as str]))

(defn start-dispatch [side unit-id]
  {:from side
   :to unit-id})

(defn add-move-order [dispatch route]
  (assoc dispatch :move route))

(defn add-attack-order [dispatch defender-id]
  (assoc dispatch :attack defender-id))

(defn- dispatch->order [{:keys [from to move attack]}]
  (cond-> []
    move (conj [:move from to move])
    attack (conj [:attack from to attack])))

(defn send-order [game-state]
  (-> game-state
      (assoc :order-queue (dispatch->order (:dispatch game-state)))
      (dissoc :dispatch)))

(defn print-dispatch [{:keys [from to move attack]}]
  (str/join (cond-> [(str "From: " (name from) " To: " (name to) "\n")]
              move  (conj (str "Move to map reference " (last move)))
              (and move attack)  (conj " and ")
              attack (conj (str "Attack " (name attack))))))
