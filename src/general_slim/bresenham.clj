(ns general-slim.bresenham)

(defn- point-add [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn- octal-points [[x y]]
  (set (let [y- (- y) x- (- x)]
         [[x,y], [x y-] [x- y] [x- y-]
          [y x], [y- x] [y x-] [y- x-]])))

(defn- next-point [[x y] d]
  (if (> d 0)
    [(inc x) (dec y)]
    [(inc x) y]))

(defn- d-make [a]
  (fn [d x y]
    (if (< d 0) (+ d (* a (inc x)) a)
        (+ d (* a (- (inc x) y)) a))))

(defn- bresenham-circle* [[x y] d points d-fn]
  (if (>= x y)
    (conj points [x y])
    (recur (next-point [x y] d)
           (d-fn d x y)
           (conj points [x y])
           d-fn)))

(defn bresenham-circle
  ([rad] (bresenham-circle [0 0] rad))
  ([[x-center y-center] rad]
   (->> (bresenham-circle* [0 rad] (- 2 (* 2 rad)) [] (d-make 2))
        (mapcat octal-points)
        (map #(point-add [x-center y-center] %))
        (sort))))

(defn bresenham-line [[x-start y-start] [x-end y-end]]
  (let [dx (Math/abs (- x-end x-start)) dy (- (Math/abs (- y-end y-start)))
        x-move (if (< x-start x-end) inc dec)
        y-move (if (< y-start y-end) inc dec)]
    (loop [e (+ dx dy) x' x-start y' y-start points [] i 0]
      (if (and (= x' x-end) (= y' y-end))  (conj points [x' y'])
          (let [x-change (>= (* 2 e) dy)  y-change (<= (* 2 e) dx)]
            (recur (cond-> e x-change (+ dy) y-change (+ dx))
                   (cond-> x' x-change x-move) (cond-> y' y-change y-move)
                   (conj points [x' y']) (inc i)))))))

(defn- draw-points [points]
  (let [[xs ys] (apply map vector points)
        x-max (apply max xs) x-min (apply min xs)
        y-max (apply max ys) y-min (apply min ys)
        grid (vec (repeat (- (inc y-max) y-min) (vec (repeat (- (inc x-max) x-min) "."))))]
    (doall (map println
                (reduce (fn [g [x y]]
                          (assoc-in g [(- y y-min) (- x x-min)] "#"))
                        grid
                        points)))))

(comment
  (draw-points (bresenham-circle [5 5] 4))
  (draw-points (bresenham-circle [5 5] 5)))

(comment
  (draw-points (bresenham-line [0 0] [7 5]))
  (draw-points (bresenham-line [6 3] [3 -5])))