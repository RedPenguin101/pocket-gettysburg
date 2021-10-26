(ns general-slim.game-states
  (:require [general-slim.field :as field]
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
                           :y (forces/make-unit :cavalry :red :y [2 2])}}
                  {:units {:v (forces/make-unit :infantry :blue :v [3 1])
                           :w (forces/make-unit :cavalry :blue :w [3 2])}}))

(def aw-ft1 (state-builder
             (-> (field/flat-field 15 10)
                 (field/add-terrain
                  :trees
                  [[3 0] [4 0] [2 1] [3 1] [12 1] [13 2] [14 3]
                   [0 6] [1 7]])
                 (field/add-terrain
                  :mountains
                  [[0 0] [1 0] [2 0] [9 0] [10 0] [11 0] [12 0] [13 0] [14 0]
                   [0 1] [1 1] [14 1] [13 1] [11 1] [10 1]
                   [0 2] [14 2]
                   [0 4] [14 5]
                   [14 6] [13 6]
                   [0 7] [14 7] [13 7] [12 7]
                   [0 8] [1 8] [2 8] [6 8] [14 8] [13 8] [12 8] [11 8]
                   [0 9] [1 9] [2 9] [3 9] [5 9] [6 9] [7 9] [9 9] [10 9] [11 9] [12 9] [13 9] [14 9]]))
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
