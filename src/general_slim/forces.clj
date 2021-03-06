(ns general-slim.forces)

(require '[clojure.test :refer [deftest is]])

(defn make-unit [type side id name short pos unit-templates]
  (assoc (type unit-templates)
         :id id :side side :position pos :unit-name name :short-name short))

;; Operations on a single unit
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset [unit]
  (assoc unit
         :move-points (:max-move-points unit)
         :move-over false))

(defn defence-value [unit terrain]
  (get-in unit [:terrain-defense terrain]))

(defn can-move? [unit]
  (> (:move-points unit) 0))

(defn move [unit location]
  (assoc unit :position location))

;; Operations on a map of units
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-side [side]
  (fn  [units-map]
    (into {} (filter (fn [[_ unit]] (= side (:side unit))) units-map))))

(deftest y
  (let [units {:abc {:id :abc :side :red :position [1 1]}
               :def {:id :def :side :blue :position [2 2]}}]
    (is (= (select-keys units [:abc])
           ((filter-side :red) units)))
    (is (= (select-keys units [:def])
           ((filter-side :blue) units)))
    (is (= {}
           ((filter-side :green) units)))))

;; Various ways to get units from a game-state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all
  "Returns a map of every id->unit
   The two arity version returns only units of one side"
  ([game-state] (:units game-state))
  ([game-state side] ((filter-side side) (all game-state))))

(defn by-location
  ([game-state] (into {} (map (fn [unit] [(:position unit) unit]) (vals (all game-state)))))
  ([game-state side] ((filter-side side) (by-location game-state))))

(defn at-locations [game-state locations]
  (into {} (filter (fn [[k _v]] (locations k)) (by-location game-state))))

(defn at-location [game-state location]
  (second (first (into {} (filter (fn [[k _v]] (#{location} k)) (by-location game-state))))))

(defn with-id [game-state id]
  (second (first (into {} (filter (fn [[k _v]] (id k)) (all game-state))))))

(deftest x
  (let [gs {:units {:abc {:id :abc :side :red :position [1 1]}
                    :def {:id :def :side :blue :position [2 2]}}}]
    (is (= (:units gs)
           (all gs)))
    (is (= (select-keys (:units gs) [:abc])
           (all gs :red)))
    (is (= (select-keys (:units gs) [:def])
           (all gs :blue)))
    (is (= {[1 1] (:abc (:units gs))
            [2 2] (:def (:units gs))}
           (by-location gs)))
    (is (= {[1 1] (:abc (:units gs))}
           (by-location gs :red)))
    (is (= {[2 2] (:def (:units gs))}
           (by-location gs :blue)))
    (is (= {[1 1] (:abc (:units gs))
            [2 2] (:def (:units gs))}
           (at-locations gs #{[1 1] [2 2]})))
    (is (= (:abc (:units gs))
           (at-location gs [1 1])))
    (is (= (:def (:units gs))
           (at-location gs [2 2])))
    (is (nil? (at-location gs [3 3])))))

(defn add-to-unit [game-state unit-id k v]
  (assoc-in game-state [:units unit-id k] v))
