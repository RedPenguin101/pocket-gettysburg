(ns general-slim.intel-test
  (:require [clojure.test :refer [deftest is testing]]
            [general-slim.intel :as SUT]
            [clojure.spec.test.alpha :as stest]
            [general-slim.specs]
            [clojure.spec.alpha :as spec]))

(comment
  (stest/abbrev-result (first (stest/check `SUT/update-intelligence)))

  (last (spec/exercise-fn `SUT/update-intelligence)))

(deftest test-update-unit-intel

  (stest/instrument `SUT/units-in-fov-of)
  (stest/instrument `SUT/age-intel)
  (stest/instrument `SUT/update-intelligence)

  (testing "degenerate cases"
    (let [gs {:red {:units {:a {:id :a :side :red :position [0 0]}}}}]
      (is (= (SUT/update-unit-intel gs :a)
             (assoc-in gs [:red :units :a :intel] {})))))

  (testing "sad-path"
    (is (thrown? clojure.lang.ExceptionInfo (SUT/update-unit-intel {} :a))))

  (testing "main"
    (let [game-state {:ticks 100
                      :red {:units {:a {:position [1 1] :id :a
                                        :side :red
                                        :viewshed #{[3 3] [5 5]}
                                        :intel {:b {:id :b
                                                    :position [3 2]
                                                    :sight-time 1
                                                    :side :red}
                                                :c {:id :c
                                                    :position [4 5]
                                                    :sight-time 1
                                                    :side :blue}}}
                                    :b {:position [2 2] :id :b :side :red}}}
                      :blue {:units {:c {:position [3 3] :id :c :side :blue}
                                     :d {:position [4 4] :id :d :side :blue}
                                     :e {:position [5 5] :id :e :side :blue}}}}]
      (is (= {:b {:side :red :id :b,
                  :position [3 2],
                  :sight-time 1
                  :is-current false} ;; b is not in the current viewshed so is aged by 1
              :c {:side :blue :id :c,
                  :position [3 3],
                  :sight-time 100
                  :is-current true} ;; c is in viewshed, so the position is updated to it's current position and age is 0
              :e {:side :blue :id :e,
                  :position [5 5],
                  :sight-time 100
                  :is-current true} ;; e was not in the old intel set, so is brought in as new
                                    ;; d is neither in the viewshed, not in old intel, so remains absent 
              }
             (get-in (SUT/update-unit-intel game-state :a)
                     [:red :units :a :intel]))))))
