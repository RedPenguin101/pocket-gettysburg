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

(def basic {:field (load-map "flat_field")
            :red forces/red
            :blue forces/blue
            :turn :red
            :turn-number 0})

(def all-visuals (state-builder
                  (load-map "all_visuals")
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

(def ready-to-attack {:field (load-map "ready_to_attack")
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

(def trees {:field (load-map "trees")
            :field-size [10 10]
            :red {:units {:y (forces/make-unit :infantry :red :y [5 6])}}
            :blue {:units {}}
            :turn :red
            :turn-number 0})

(def mountains {:field (load-map "mountains")
                :field-size [10 10]
                :red {:units {:y (forces/make-unit :infantry :red :y [5 6])}}
                :blue {:units {}}
                :turn :red
                :turn-number 0})

;; road dirs
[:hor :vert :up-right :up-left :down-right :down-left]

(def road {:field (load-map "road")
           :field-size [10 10]
           :red {:units {:x (forces/make-unit :cavalry :red :x [1 7])
                         :y (forces/make-unit :cavalry :red :y [8 6])}}
           :blue {:units {}}
           :turn :red
           :turn-number 0})
