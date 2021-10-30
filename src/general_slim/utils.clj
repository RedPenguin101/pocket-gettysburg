(ns general-slim.utils)

(defn map-vals [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn update-vals [m f & args]
  (map-vals #(apply f % args) m))

(defn dissoc-in [m ks k]
  (update-in m ks dissoc k))

(comment
  (dissoc-in {:hello {:world {:foo "bar" :baz 3}}}
             [:hello :world] :baz))

(defn coordinate
  "Turns a 2d grid (coll of coll of x) into a map of coord->x"
  [grid]
  (into {} (apply concat (map-indexed (fn [y x-row] (map-indexed (fn [x v] [[x y] v]) x-row)) grid))))

(defn average [xs]
  (float (/ (apply + xs)
            (count xs))))

(defn zero-floored-minus [x y]
  (max 0 (- x y)))

(def opposing-dirs
  {:up :down :down :up :left :right :right :left})

(defn relative-coord [[x y] dir]
  (case dir
    :up [x (dec y)]
    :down [x (inc y)]
    :right [(inc x) y]
    :left [(dec x) y]))

(defn relative-position
  "Given 2 adjacent points p1 and p2, returns the relative position of
   p2 to p1. i.e. if p2 is above p1, returns :up"
  [[x1 y1] [x2 y2]]
  (let [rel-pos [(- x1 x2) (- y1 y2)]]
    (case rel-pos
      [-1 0] :left
      [1 0] :right
      [0 1] :up
      [0 -1] :down
      (throw (ex-info "Two points are not adjacent" {:p1 [x1 y1] :p2 [x2 y2]})))))

(defn manhattan [[x y] dist]
  (set (for [d (range 0 (inc dist))
             x' (range (- d) (inc d))
             y' (range (- d) (inc d))
             :when (= d (+ (Math/abs x') (Math/abs y')))]
         [(+ x x') (+ y y')])))

(defn adjacent? [p1 p2]
  ((manhattan p1 1) p2))

(defn coord+ [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn coord- [[x1 y1] [x2 y2]]
  [(- x1 x2) (- y1 y2)])

(defn remove-oob-coords [x-max y-max coords]
  (remove (fn [[x y]] (or (< x 0) (< y 0)
                          (>= x x-max) (>= y y-max)))
          coords))

(defn in-view? [[c-x c-y] [x y]]
  (and (<= c-x x (+ c-x 14))
       (<= c-y y (+ c-y 14))))
