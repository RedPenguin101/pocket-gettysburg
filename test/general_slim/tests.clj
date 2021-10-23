(ns general-slim.tests
  (:require [clojure.test :refer [is deftest]]
            [general-slim.game :as SUT]
            [general-slim.field :as field]
            [general-slim.forces :as forces]))

(def basic-start-state
  {:field (field/flat-field 10 10)
   :red forces/red
   :blue forces/blue
   :turn :red
   :turn-number 0})

(deftest end-turn
  (is (= :blue (:turn (SUT/main-loop (assoc basic-start-state :order [:end-turn :red])))))
  (is (thrown? Exception (SUT/main-loop (assoc basic-start-state :order [:end-turn :blue])))))

(deftest moving-units
  (is (= [2 3]
         (get-in (SUT/main-loop (assoc basic-start-state :order [:move :red :inf1 [2 3]]))
                 [:red :units :inf1 :position])))
  ;; can't move to non-adjacent square
  (is (= [2 2]
         (get-in (SUT/main-loop (assoc basic-start-state :order [:move :red :inf1 [3 3]]))
                 [:red :units :inf1 :position])))
  ;; can't move to occupied square
  (is (= [2 3]
         (get-in (-> basic-start-state
                     (assoc-in [:red :units :inf1 :position] [2 3])
                     (assoc :order [:move :red :inf1 [3 3]])
                     (SUT/main-loop))
                 [:red :units :inf1 :position])))
  ;; can't move if not enough move points
  (is (= [2 2]
         (get-in (-> basic-start-state
                     (assoc-in [:red :units :inf1 :move-points] 0)
                     (assoc :order [:move :red :inf1 [2 3]])
                     (SUT/main-loop))
                 [:red :units :inf1 :position]))))

(def ready-to-attack {:field (field/flat-field 10 10)
                      :red {:units {:inf1 {:id :inf1 :unit-type :infantry :hp 10
                                           :position [6 6] :side :red :move-points 1}}}
                      :blue {:units {:inf1 {:id :inf1 :unit-type :infantry :hp 10
                                            :position [7 6] :side :blue :move-points 1}}}
                      :turn :red
                      :turn-number 0
                      :cursor [5 5]})

(deftest attacking-units
  (is (empty? (get-in (SUT/tick (assoc ready-to-attack :order [:attack :red :inf1 :inf1]))
                      [:blue :units]))))

