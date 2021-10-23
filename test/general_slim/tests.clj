(ns general-slim.tests
  (:require [clojure.test :refer [is deftest]]
            [general-slim.game :as SUT]))

(def basic-start-state
  {:field (general-slim.field/flat-field 10 10)
   :red general-slim.forces/red
   :blue general-slim.forces/blue
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