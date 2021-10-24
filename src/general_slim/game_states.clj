(ns general-slim.game-states
  (:require [general-slim.field :as field]
            [general-slim.forces :as forces]))

(def basic {:field (field/flat-field 10 10)
            :red forces/red
            :blue forces/blue
            :turn :red
            :turn-number 0
            :cursor [5 5]})

(def ready-to-attack {:field (field/flat-field 10 10)
                      :red {:units {:inf1 (forces/make-unit :infantry :red :inf1 [6 6])
                                    :cav1 (forces/make-unit :cavalry :red :cav1 [3 3])}}
                      :blue {:units {:inf1 (forces/make-unit :infantry :blue :inf1 [7 6])}}
                      :turn :red
                      :turn-number 0})

(def trees {:field (-> (field/flat-field 10 10)
                       (assoc [4 6] {:grid [4 6] :terrain :trees})
                       (assoc [5 4] {:grid [5 4] :terrain :trees})
                       (assoc [5 9] {:grid [5 9] :terrain :trees}))
            :red {:units {:y (forces/make-unit :infantry :red :y [5 6])}}
            :blue {:units {}}
            :turn :red
            :turn-number 0})

(def mountains {:field (-> (field/flat-field 10 10)
                           (assoc [4 6] {:grid [4 6] :terrain :mountains})
                           (assoc [5 4] {:grid [5 4] :terrain :mountains})
                           (assoc [5 9] {:grid [5 9] :terrain :mountains}))
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
           :red {:units {:x (forces/make-unit :cavalry :red :x [1 7])
                         :y (forces/make-unit :cavalry :red :y [8 6])}}
           :blue {:units {}}
           :turn :red
           :turn-number 0})
