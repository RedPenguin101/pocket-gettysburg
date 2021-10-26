(ns general-slim.map-loader
  (:require [clojure.string :as str]
            [general-slim.utils :refer [coordinate]]))

(def terrain-map
  {\^ :mountains
   \T :trees
   \. :field
   \_ :road})

(defn make-tile [terrain]
  (cond-> {:terrain terrain}
    (= :road terrain) (assoc :dirs [:hor :vert])))

(defn row->terrain [row]
  (mapv (comp make-tile terrain-map) row))

(defn add-in-grids [[k v]]
  [k (assoc v :grid k)])

(defn load-map [map-name]
  (->> (str "resources/maps/" map-name ".txt")
       (slurp)
       (str/split-lines)
       (mapv row->terrain)
       (doall)
       (coordinate)
       (map add-in-grids)
       (into {})))

(comment
  (load-map "aw_ft1"))