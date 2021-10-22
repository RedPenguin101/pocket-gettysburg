(ns general-slim.field)

(defn flat-field [x-size y-size]
  (into {} (for [x (range 0 x-size) y (range 0 y-size)]
             [[x y] {:grid [x y]
                     :elevation 0
                     :underfoot :field}])))

