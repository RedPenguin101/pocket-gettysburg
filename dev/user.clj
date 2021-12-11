(ns user
  (:require [portal.api :as p]
            [general-slim.ui :as ui]))

(def debug ui/debug)

(defn start! []
  (def p (p/open {:launcher :vs-code}))
  (add-tap #'p/submit))

(comment
  (start!)
  (tap> @debug)
  (prn @p))

(get-in @debug [:red :units])