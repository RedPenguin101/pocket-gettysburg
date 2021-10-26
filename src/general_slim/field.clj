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

(defn add-terrain [field terrain locs]
  (reduce (fn [fld loc]
            (assoc fld loc {:grid loc :terrain terrain}))
          field
          locs))

(defn add-road [field path]
  (reduce (fn [fld loc]
            (assoc fld loc {:grid loc :terrain :road :dirs [:hor :vert]}))
          field
          path))

(defn build-map [x-size y-size trees]
  (merge (flat-field x-size y-size)
         (random-trees x-size y-size trees)))

(defn field-size [field]
  (->> field keys
       (apply map vector)
       (map #(inc (apply max %)))))

(defn terrain-map
  "returns the terrain at each coordinate provided"
  [field coords]
  (into {} (map (juxt identity #(get-in field [% :terrain])) coords)))
