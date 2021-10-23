(ns general-slim.utils)

(defn map-vals [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn dissoc-in [m ks k]
  (update-in m ks dissoc k))

(dissoc-in {:hello {:world {:foo "bar" :baz 3}}}
           [:hello :world] :baz)

