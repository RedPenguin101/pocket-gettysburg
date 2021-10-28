(ns general-slim.field)

(defn flat-field [x-size y-size]
  (into {} (for [x (range 0 x-size) y (range 0 y-size)]
             [[x y] {:grid [x y]
                     :terrain :field}])))

(defn field-size [field]
  (->> field keys
       (apply map vector)
       (map #(inc (apply max %)))))

(defn terrain-at [field coord]
  (get-in field [coord :terrain]))

(defn terrain-map
  "returns the terrain at each coordinate provided"
  ([field] (into {} (map (fn [[k v]] [k (:terrain v)]) field)))
  ([field coords]
   (into {} (map (juxt identity #(get-in field [% :terrain])) coords))))
