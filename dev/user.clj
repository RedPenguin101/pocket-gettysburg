(ns user
  (:require [portal.api :as p]
            [general-slim.ui :as ui]
            [general-slim.combat :as combat]
            [general-slim.forces :as f]
            [general-slim.inputs :as i]
            [general-slim.utils :as u]))



(defn start! []
  (def p (p/open {:launcher :vs-code}))
  (add-tap #'p/submit))

(comment
  (get-in @debug [:red :units])
  (start!)
  (tap> @debug)
  (prn @p))

(comment
  "Starting a game"

  (ui/go)
  "Debug"
  (require '[general-slim.forces :as forces]
           '[general-slim.inputs :as inputs]
           '[general-slim.utils :as u])
  (def game-state @ui/debug)
  (keys debug)

  (keys (get-in game-state [:red :units "3a41c465-6141-40e2-a1fd-cf581a80f91c"]))

  (forces/occupied-grids game-state :blue)
  ;; => #{[13 8] [9 4]}
  (u/manhattan [8 4] 1)
  ;; => #{[8 4] [7 4] [8 3] [8 5] [9 4]}
  (:viewshed game-state)
  ;; => nil
  (get-in game-state [:red :units "3a41c465-6141-40e2-a1fd-cf581a80f91c"]))