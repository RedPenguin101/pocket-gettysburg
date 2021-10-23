(ns general-slim.tests
  (:require [clojure.test :refer [is deftest]]
            [general-slim.game :as SUT]))

(def basic-start-state
  {:field (general-slim.field/flat-field 10 10)
   :red general-slim.forces/side2
   :blue general-slim.forces/side1
   :turn :red
   :turn-number 0})

(deftest end-turn
  (is (= :blue (:turn (SUT/main-loop (assoc basic-start-state :order [:end-turn :red])))))
  (is (thrown? Exception (SUT/main-loop (assoc basic-start-state :order [:end-turn :blue])))))

(deftest moving-units
  (is (= [6 7]
         (get-in (SUT/main-loop (assoc basic-start-state :order [:move :red :inf1 [6 7]]))
                 [:red :units :inf1 :position])))
  ;; can't move to non-adjacent square
  #_(is (thrown? Exception (SUT/main-loop (assoc basic-start-state :order [:move :red :inf1 [6 6]])))))