(ns general-slim.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.edn :as edn]))


(s/def ::coord (s/coll-of pos-int? :kind vector? :count 2))
(s/def ::tile (s/keys :req-un [::grid ::terrain]))
(s/def ::field (s/map-of ::coord ::tile))

(s/def ::movement-table (s/map-of keyword? (s/and number? pos?)))

(s/def ::unit
  (s/keys :req-un [::short-name
                   ::move-points
                   ::max-move-points
                   ::unit-type
                   ::soldiers
                   ::movement-table
                   ::move-over
                   ::id
                   ::side
                   ::unit-name
                   ::position]))

(defn uuid-str? [s]
  (try (java.util.UUID/fromString s)
       (catch IllegalArgumentException _ false)))

(s/def ::units (s/map-of uuid-str? ::unit))

(s/def ::game-state
  (s/keys :req-un [::turn ::turn-number
                   ::red ::blue
                   ::field]))

(comment
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
