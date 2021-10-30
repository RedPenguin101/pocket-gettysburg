(ns general-slim.field)

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

(defn- complete? [field]
  (let [[x-size y-size] (field-size field)
        expected-coords (for [x (range 0 x-size) y (range 0 y-size)] [x y])]
    (every? #(contains? field %) expected-coords)))

(defn valid-field? [field]
  (and (not-empty field)
       (not-any? neg? (apply concat (keys field)))
       (complete? field)))
