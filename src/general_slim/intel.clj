(ns general-slim.intel
  (:require [general-slim.forces :as forces]
            [clojure.spec.alpha :as spec]))

(defn- units-in-fov-of [game-state unit]
  (->> (:viewshed unit)
       (forces/units-at-locations game-state)
       vals))

(spec/fdef units-in-fov-of
  :args (spec/cat
         ::game-state map?
         ::unit (spec/nilable :general-slim.specs/unit))
  :ret (spec/coll-of :general-slim.specs/unit))

(defn- age-intel [intel]
  (update-vals intel #(update % :age inc)))

(spec/fdef age-intel
  :args (spec/cat ::intel (spec/map-of some? :general-slim.specs/intel-report))
  :ret (spec/map-of some? :general-slim.specs/intel-report))

(defn- update-intelligence [old-intel units sight-time]
  (merge
   (when old-intel (update-vals old-intel #(assoc % :is-current false)))
   (update-vals (->> units
                     (map #(vector (:id %) (select-keys % [:position :id :side])))
                     (into {}))
                #(assoc % :sight-time sight-time
                        :is-current true))))

(spec/fdef update-intelligence
  :args (spec/cat ::old-intel (spec/nilable (spec/map-of :general-slim.specs/id :general-slim.specs/intel-report))
                  ::units (spec/nilable (spec/coll-of :general-slim.specs/unit))
                  ::sight-time int?)
  :ret (spec/map-of :general-slim.specs/id :general-slim.specs/intel-report))

(defn update-unit-intel [game-state unit-id]
  (let [unit (forces/unit-with-id game-state unit-id)]
    (if unit
      (update-in game-state [(:side unit) :units unit-id :intel]
                 update-intelligence
                 (units-in-fov-of game-state unit)
                 (or (:ticks game-state) 0))
      (throw (ex-info (str "Unit " unit-id " not in game-state")
                      game-state)))))

(defn update-all-unit-intel [game-state]
  (reduce update-unit-intel
          game-state
          (map :id (forces/units game-state))))
