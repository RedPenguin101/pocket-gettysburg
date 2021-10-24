(ns general-slim.route-calc
  (:require [ubergraph.core :as ug]
            [ubergraph.alg :as alg]))

(defn adjacent? [[x1 y1] [x2 y2]]
  (and (<= (Math/abs (- x1 x2)) 1)
       (<= (Math/abs (- y1 y2)) 1)
       (not (and (not= x1 x2) (not= y1 y2)))))

(defn build-graph [nodes]
  (ug/add-directed-edges*
   (ug/digraph)
   (for [this-grid (map first nodes)
         [other-grid mv] nodes
         :when (adjacent? this-grid other-grid)]
     [this-grid other-grid mv])))

(defn shortest-path [graph start end]
  (:cost (alg/shortest-path graph {:start-node start
                                   :end-node end
                                   :cost-attr :weight})))

(defn accessible-squares
  "Given a sequence of coords with their movement costs,
   as well as the unit's location and movement points
   will return a set of the squares that can be reached
   by the unit on that turn"
  [current move-points target-points]
  (let [graph (build-graph target-points)]
    (->> (map first target-points)
         (map #(vector % (shortest-path graph current %)))
         (filter #(>= move-points (second %)))
         (map first) ;; get coord only
         (set))))

(comment
  (let [cr [5 6] mp 3
       ;; grids within 3 squares of 5,6, with trees at 3 locations
        tp [[[2 6] 1] [[3 5] 1] [[3 6] 1] [[3 7] 1] [[4 4] 1] [[4 5] 1] [[4 6] 3] [[4 7] 1] [[4 8] 1] [[5 3] 1] [[5 4] 3] [[5 5] 1] [[5 6] 1] [[5 7] 1] [[5 8] 1] [[5 9] 3] [[6 4] 1] [[6 5] 1] [[6 6] 1] [[6 7] 1] [[6 8] 1] [[7 5] 1] [[7 6] 1] [[7 7] 1] [[8 6] 1]]]
    (count (accessible-squares cr mp tp)))
  ;; => 20
  )
