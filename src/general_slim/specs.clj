(ns general-slim.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

;; general/utility
(s/def ::atom (s/or ::string string? ::number number? ::keyword keyword?))
(s/def ::not-neg-int (s/or :zero zero? :pos-int pos-int?))
(s/def ::not-neg-num (s/or :zero zero? :pos-int pos?))
(s/def ::coord (s/coll-of int? :kind vector? :count 2))

;; units
(s/def ::movement-table (s/map-of keyword? (s/and number? pos?)))
(s/def ::id ::atom)
(s/def ::side keyword?)
(s/def ::unit-type keyword?)
(s/def ::position ::coord)
(s/def ::short-name string?)
(s/def ::unit-name string?)
(s/def ::move-points ::not-neg-num)
(s/def ::max-move-points ::not-neg-num)
(s/def ::soldiers pos-int?)
(s/def ::move-over boolean?)
(s/def ::unit
  (s/keys :req-un [::id
                   ::side
                   ::position]
          :opt-un [::short-name
                   ::move-points
                   ::max-move-points
                   ::unit-type
                   ::soldiers
                   ::movement-table
                   ::move-over
                   ::unit-name]))

(comment
  (gen/sample (s/gen ::move-points))
  (last (gen/sample (s/gen ::unit)))
  (s/exercise ::position))

(defn uuid-str? [s]
  (try (java.util.UUID/fromString s)
       (catch IllegalArgumentException _ false)))

(s/def ::tile (s/keys :req-un [::grid ::terrain]))
(s/def ::field (s/map-of ::coord ::tile))

(s/def ::units (s/map-of uuid-str? ::unit))

(s/def ::game-state
  (s/keys :req-un [::turn ::turn-number
                   ::red ::blue
                   ::field]))

;; intel reports

(s/def ::sight-time ::not-neg-int)
(s/def ::is-current boolean?)
(s/def ::intel-report
  (s/keys :req-un [::id ::position ::sight-time ::side]
          :opt-un [::is-current]))

(comment
  (s/valid? (s/map-of some? ::intel-report)
            {:b {:id :b, :position [3 2], :age 4},
             :c {:id :c, :position [3 3], :age 0},
             :e {:id :e, :position [5 5], :age 0}})

  (s/valid? ::movement-table {:field 1, :road 0.5, :trees 1, :mountains 2})

  (s/valid? ::units {"c864afe-6f34-4161-a73c-5885ec915958"
                     {:short-name "inf1",
                      :move-points 3,
                      :max-move-points 3,
                      :unit-type :infantry,
                      :soldiers 1000,
                      :movement-table {:field 1, :road 0.5, :trees 1, :mountains 2},
                      :move-over false,
                      :id "c8634afe-6f34-4161-a73c-5885ec915958",
                      :side :blue,
                      :unit-name "inf1",
                      :position [7 7]},
                     "924b45a5-09f6-45e1-a28c-68ae2a533cb2"
                     {:short-name "inf2",
                      :move-points 3,
                      :max-move-points 3,
                      :unit-type :infantry,
                      :soldiers 1000,
                      :movement-table {:field 1, :road 0.5, :trees 1, :mountains 2},
                      :move-over false,
                      :id "924b45a5-09f6-45e1-a28c-68ae2a533cb2",
                      :side :blue,
                      :unit-name "inf2",
                      :position [8 8]}}))

