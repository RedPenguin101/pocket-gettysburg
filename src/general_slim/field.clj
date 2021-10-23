(ns general-slim.field)

(defn rand-int-between [min max]
  (repeatedly #(+ min (rand-int (- max min)))))

(defn flat-field [x-size y-size]
  (into {} (for [x (range 0 x-size) y (range 0 y-size)]
             [[x y] {:grid [x y]
                     :terrain :field}])))

(defn random-trees [x-size y-size num]
  (let [xs (take num (rand-int-between 0 x-size))
        ys (take num (rand-int-between 0 y-size))]
    (into {} (map (fn [x y] [[x y] {:grid [x y]
                                    :terrain :trees}])
                  xs ys))))

(defn build-map [x-size y-size trees]
  (merge (flat-field x-size y-size)
         (random-trees x-size y-size trees)))

(build-map 10 10 5)