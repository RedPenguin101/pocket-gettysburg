(ns general-slim.map-loader
  "This namespace loads map resources.
   A map resource is a textual representation
   (txt files) consisting of the different available
   terrain types, with the position of the glyph in the
   file corresponding to its coordinate in the map.

   For example, the below string shows a map
   resources with field (.), Forests (T),
   Mountain (^) and roads (_)
   
   ^^^TT....^^^^^^
   ^^TT......^^T^^
   ^............T^
   ..............T
   ^......________
   ________......^
   T............^^
   ^T..........^^^
   ^^^...^....^^^^
   ^^^^.^^^.^^^^^^
   
   The functions in this namespace turn this 
   into the games data-representation of a 'Field',
   which is a hashmap of vector->tile,
   where the tile is a hashmap. For example
   
   {:grid [0 5], :dirs [:hor], :terrain :road}

   (note the direction is unique to the road 
   terrain type)
   
   See Specs for complete field description"

  (:require [clojure.string :as str]
            [clojure.set :as set]
            [general-slim.field :as field]
            [general-slim.utils :refer [valid-directions map-vals coordinate]]))

(def terrain-map
  {\^ :mountains
   \T :trees
   \. :field
   \_ :road})

(defn- row->terrain [row]
  (mapv (comp #(hash-map :terrain %) terrain-map) row))

(defn- add-in-grids [[k v]]
  [k (assoc v :grid k)])

(defn- road-dirs->road-tile [dirs]
  (cond (= 4 (count dirs)) [:hor :vert]
        (= 1 (count dirs)) (if (#{:up :down} (first dirs)) [:vert] [:hor])
        :else (cond-> []
                (set/subset? #{:right :left} dirs) (conj :hor)
                (set/subset? #{:up :down} dirs) (conj :vert)
                (set/subset? #{:left :up} dirs) (conj :ul)
                (set/subset? #{:left :down} dirs) (conj :dl)
                (set/subset? #{:right :up} dirs) (conj :ur)
                (set/subset? #{:right :down} dirs) (conj :dr))))

(defn- road-builder [field]
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

(comment
  (load-map "aw_ft1")
  ;; => {[8 8] {:grid [8 8], :terrain :field},
  ;;     [7 6] {:grid [7 6], :terrain :field},
  ;;     [11 9] {:grid [11 9], :terrain :mountains},
  ;;     [8 4] {:grid [8 4], :dirs [:hor], :terrain :road},
  ;;     [13 2] {:grid [13 2], :terrain :trees},
  ;;     ...} etc.
  )
