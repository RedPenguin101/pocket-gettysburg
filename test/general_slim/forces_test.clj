(ns general-slim.forces-test
  (:require [clojure.test :refer [deftest is are testing]]
            [general-slim.forces :as SUT]
            [clojure.spec.test.alpha :as stest]
            [general-slim.specs]))

(deftest test-unit-at-locations
  (stest/instrument `SUT/units-at-locations)
  (testing "empty and degenerate cases"
    (are [game-state coords] (empty? (SUT/units-at-locations game-state coords))
      {} #{}
      {} #{[1 1]}
      {:red {} :blue {}} #{[1 1]}))

  (testing "input spec failures"
    (are [game-state coords] (thrown? Exception (SUT/units-at-locations game-state coords))
      nil #{[1 1]}    ;; game-state must be a map
      {} [1 1]        ;; a COLLECTION of coords must be passed in
      ))

  (testing ""
    (let [game-state {:red {:units {:a {:position [1 1] :id :a}
                                    :b {:position [2 2] :id :b}}}}]
      (is (= {[1 1] {:position [1 1] :id :a}}
             (SUT/units-at-locations game-state #{[1 1]})))
      (is (= {[2 2] {:position [2 2] :id :b}}
             (SUT/units-at-locations game-state #{[2 2]})))
      (is (= {[1 1] {:position [1 1] :id :a}
              [2 2] {:position [2 2] :id :b}}
             (SUT/units-at-locations game-state #{[1 1] [2 2]})))
      (is (empty? (SUT/units-at-locations game-state #{[1 2]}))))))
