(ns general-slim.utils)

(defn map-vals [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn update-vals [m f & args]
  (map-vals #(apply f % args) m))

(defn dissoc-in [m ks k]
  (update-in m ks dissoc k))

(dissoc-in {:hello {:world {:foo "bar" :baz 3}}}
           [:hello :world] :baz)

(defn coordinate
  "Turns a 2d grid (coll of coll of x) into a map of coord->x"
  [grid]
  (into {} (apply concat (map-indexed (fn [y x-row] (map-indexed (fn [x v] [[x y] v]) x-row)) grid))))