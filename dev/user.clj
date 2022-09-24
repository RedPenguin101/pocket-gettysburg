(ns user
  (:require [portal.api :as p]
            [general-slim.ui :as ui]
            [general-slim.combat :as combat]
            [general-slim.forces :as f]
            [general-slim.inputs :as i]))



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
  (def debug @ui/debug)
  (keys debug)
  (:cursor debug)
  (require '[general-slim.forces :as f]
           '[general-slim.inputs :as i])
  (f/unit-at-location debug [5 3])
  (:turn debug)
  (let [u (f/unit-at-location debug [5 3])]
    (f/can-move? u)
    (i/can-move-to debug u)))