(ns general-slim.game-states
  (:require [general-slim.field :as field]
            [general-slim.map-loader :refer [load-map]]
            [general-slim.forces :as forces]))

(defn state-builder [field reds blues]
  {:field field
   :field-size (field/field-size field)
   :red reds :blue blues
   :turn :red
   :turn-number 0})

(def basic {:field (field/flat-field 10 10)
            :field-size [10 10]
            :red forces/red
            :blue forces/blue
            :turn :red
            :turn-number 0})

(def all-visuals (state-builder
                  (-> (field/flat-field 10 10)
                      (field/add-terrain :trees [[1 1]])
                      (field/add-terrain :mountains [[1 2]])
                      (field/add-road [[4 0] [4 1] [4 2] [4 3] [4 4] [4 5] [4 6] [4 7] [4 8]]))
                  {:units {:x (forces/make-unit :infantry :red :x [2 1])
                           :y (forces/make-unit :cavalry :red :y [2 2])
                           :z (forces/make-unit :artillery :red :z [2 3])}}
                  {:units {:v (forces/make-unit :infantry :blue :v [3 1])
                           :w (forces/make-unit :cavalry :blue :w [3 2])
                           :u (forces/make-unit :artillery :blue :u [3 3])}}))

(def aw-ft1 (state-builder
             (load-map "aw_ft1")
             {:units {:x (forces/make-unit :infantry :red :x [5 3])
                      :y (forces/make-unit :infantry :red :y [3 5])}}
             {:units {:v (forces/make-unit :infantry :blue :v [13 5])
                      :w (forces/make-unit :infantry :blue :w [13 8])}}))

(def ready-to-attack {:field (-> (field/flat-field 10 10)
                                 (field/add-terrain :trees [[7 7]])
                                 (field/add-terrain :mountains [[7 8]]))
                      :field-size [10 10]
                      :red {:units {:inf1 (forces/make-unit :infantry :red :inf1 [6 6])
                                    :inf2 (forces/make-unit :infantry :red :inf2 [6 7])
                                    :inf3 (forces/make-unit :infantry :red :inf3 [6 8])
                                    :cav1 (forces/make-unit :cavalry :red :cav1 [7 3])}}
                      :blue {:units {:inf1 (forces/make-unit :infantry :blue :inf1 [7 6])
                                     :inf2 (forces/make-unit :infantry :blue :inf2 [7 7])
                                     :inf3 (forces/make-unit :infantry :blue :inf3 [7 8])}}
                      :turn :red
                      :turn-number 0})

(def multi-dir-attack {:field (field/flat-field 10 10)
                       :field-size [10 10]
                       :red {:units {:x (forces/make-unit :infantry :red :x [7 3])}}
                       :blue {:units {:y (forces/make-unit :cavalry :blue :y [7 6])
                                      :z (forces/make-unit :cavalry :blue :z [6 5])}}
                       :turn :red
                       :turn-number 0})

(def trees {:field (-> (field/flat-field 10 10)
                       (assoc [4 6] {:grid [4 6] :terrain :trees})
                       (assoc [5 4] {:grid [5 4] :terrain :trees})
                       (assoc [5 9] {:grid [5 9] :terrain :trees}))
            :field-size [10 10]
            :red {:units {:y (forces/make-unit :infantry :red :y [5 6])}}
            :blue {:units {}}
            :turn :red
            :turn-number 0})

(def mountains {:field (-> (field/flat-field 10 10)
                           (assoc [4 6] {:grid [4 6] :terrain :mountains})
                           (assoc [5 4] {:grid [5 4] :terrain :mountains})
                           (assoc [5 9] {:grid [5 9] :terrain :mountains}))
                :field-size [10 10]
                :red {:units {:y (forces/make-unit :infantry :red :y [5 6])}}
                :blue {:units {}}
                :turn :red
                :turn-number 0})

;; road dirs
[:hor :vert :up-right :up-left :down-right :down-left]

(def road {:field (-> (field/flat-field 10 10)
                      (assoc [0 7] {:grid [0 7] :terrain :road :dirs [:hor]})
                      (assoc [1 7] {:grid [1 7] :terrain :road :dirs [:hor]})
                      (assoc [2 7] {:grid [2 7] :terrain :road :dirs [:hor]})
                      (assoc [3 7] {:grid [3 7] :terrain :road :dirs [:hor]})
                      (assoc [4 7] {:grid [4 7] :terrain :road :dirs [:hor]})
                      (assoc [5 7] {:grid [5 7] :terrain :road :dirs [:hor :vert]})
                      (assoc [5 6] {:grid [5 6] :terrain :road :dirs [:se]})
                      (assoc [6 6] {:grid [6 6] :terrain :road :dirs [:nw :vert]})
                      (assoc [6 5] {:grid [6 5] :terrain :road :dirs [:sw]})
                      (assoc [6 7] {:grid [6 7] :terrain :road :dirs [:hor :vert]})
                      (assoc [7 7] {:grid [7 7] :terrain :road :dirs [:hor]})
                      (assoc [8 7] {:grid [8 7] :terrain :road :dirs [:hor]})
                      (assoc [9 7] {:grid [9 7] :terrain :road :dirs [:hor]}))
           :field-size [10 10]
           :red {:units {:x (forces/make-unit :cavalry :red :x [1 7])
                         :y (forces/make-unit :cavalry :red :y [8 6])}}
           :blue {:units {}}
           :turn :red
           :turn-number 0})
