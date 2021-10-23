(ns general-slim.utils)

(defn map-vals [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))
