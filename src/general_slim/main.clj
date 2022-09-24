(ns general-slim.main
  (:gen-class)
  (:require [general-slim.ui :refer [go]]))

(defn -main []
  (go))

(comment
  (-main))