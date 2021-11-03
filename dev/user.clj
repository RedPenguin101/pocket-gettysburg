(ns user
  (:require [portal.api :as p]
            [general-slim.ui :as ui]))

(defn start! []
  (def p (p/open {:launcher :vs-code}))
  (add-tap #'p/submit))

(comment
  (start!)
  (tap> @general-slim.ui/debug)
  (prn @p))
