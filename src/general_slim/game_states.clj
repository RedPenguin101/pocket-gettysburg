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