(ns general-slim.route-calc
  (:require [ubergraph.core :as ug]
            [ubergraph.alg :as alg]))

(defn adjacent? [[x1 y1] [x2 y2]]
  (and (<= (Math/abs (- x1 x2)) 1)
       (<= (Math/abs (- y1 y2)) 1)
       (not (and (not= x1 x2) (not= y1 y2)))))

(adjacent? [0 0] [1 0])

(defn build-graph [nodes]
  (ug/add-directed-edges*
   (ug/digraph)
   (for [this-grid (map first nodes)
         [other-grid mv] nodes
         :when (adjacent? this-grid other-grid)]
     [this-grid other-grid mv])))

(ug/pprint (build-graph [[[0 0] 1] [[1 0] 1] [[0 1] 2]
                         [[1 1] 1]]))

@(:list-of-edges
  (alg/shortest-path (build-graph [[[0 0] 1] [[1 0] 2] [[0 1] 2]
                                   [[1 1] 2]])
                     {:start-node [0 0] :end-node [1 1]
                      :cost-attr :weight}))

(defn shortest-path [graph start end]
  (:cost (alg/shortest-path graph {:start-node start :end-node end
                                   :cost-attr :weight})))

(defn prep-cmap [c-map mv-table]
  (for [[grid terr] c-map]
    [grid (terr mv-table)]))

(defn accessible-squares
  "Given a map of candidate coords with terrain type,
   with the unit's movement points and movement table
   will return a set of the squares that can be reached
   by the unit on that turn"
  [current-loc candidate-map move-points movement-table]
  (set (map first (filter #(>= move-points (second %))
                          (map #(vector % (shortest-path (build-graph (prep-cmap candidate-map movement-table))
                                                         current-loc
                                                         %))
                               (keys candidate-map))))))

(accessible-squares
 [0 0]
 {[0 0] :field
  [1 0] :trees
  [0 1] :trees
  [1 1] :field}
 2
 {:field 1 :trees 2})

(defn route-cost [route-with-terrain movement-table]
  (let [cmap (into {} (prep-cmap route-with-terrain movement-table))]
    (reduce (fn [acc loc]
              (+ acc (cmap loc)))
            route-with-terrain)))

()