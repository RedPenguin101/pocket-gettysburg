(ns general-slim.map-loader
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [general-slim.field :as field]
            [general-slim.utils :refer [valid-directions map-vals coordinate manhattan relative-position]]))

(def terrain-map
  {\^ :mountains
   \T :trees
   \. :field
   \_ :road})

(defn row->terrain [row]
  (mapv (comp #(hash-map :terrain %) terrain-map) row))

(defn add-in-grids [[k v]]
  [k (assoc v :grid k)])

(defn road-dirs->road-tile [dirs]
  (cond (= 4 (count dirs)) [:hor :vert]
        (= 1 (count dirs)) (if (#{:up :down} (first dirs)) [:vert] [:hor])
        :else (cond-> []
                (set/subset? #{:right :left} dirs) (conj :hor)
                (set/subset? #{:up :down} dirs) (conj :vert)
                (set/subset? #{:left :up} dirs) (conj :ul)
                (set/subset? #{:left :down} dirs) (conj :dl)
                (set/subset? #{:right :up} dirs) (conj :ur)
                (set/subset? #{:right :down} dirs) (conj :dr))))

(defn road-builder [field]
  (let [tmap (field/terrain-map field)
        roads (keys (filter #(= :road (second %)) tmap))
        road-tiles (->> (map (juxt identity #(valid-directions % roads)) roads)
                        (into {})
                        (map-vals road-dirs->road-tile))]
    (reduce (fn [fld [coord tiles]]
              (assoc-in fld [coord :dirs] tiles))
            field road-tiles)))

(defn load-map [map-name]
  (->> (str "resources/maps/" map-name ".txt")
       (slurp)
       (str/split-lines)
       (mapv row->terrain)
       (doall)
       (coordinate)
       (map add-in-grids)
       (into {})
       (road-builder)))
