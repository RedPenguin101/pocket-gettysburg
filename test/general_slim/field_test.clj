(ns general-slim.field-test
  (:require [clojure.test :refer [deftest is are]]
            [general-slim.field :as SUT]))

(deftest field-validity
  ;; empty field is not valid
  (is (not (SUT/valid-field? {})))
  ;; field can't contain negatives
  (is (not (SUT/valid-field? {[5 5] {} [0 0] {} [1 1] {} [-1 -1] {}})))
  ;; field must have a square for every value in its field-size
  (is (SUT/valid-field? {[0 0] {}}))
  (is (SUT/valid-field? {[0 0] {} [1 0] {}}))
  (is (not (SUT/valid-field? {[0 0] {} [1 0] {} [0 1] {}})))
  (is (SUT/valid-field? {[0 0] {} [1 0] {} [0 1] {} [1 1] {}}))
  (is (not (SUT/valid-field? {[0 0] {} [1 1] {}}))))

(deftest field-size-tests
  (are [size field] (= size (SUT/field-size field))
    [10 10] {[9 9] {}}
    [1 1]   {[0 0] {}}
    [1 2]   {[0 0] {} [0 1] {}}
    [2 1]   {[0 0] {} [1 0] {}}
    [2 1]   {[0 0] {} [1 0] {}}
    [2 2]   {[0 0] {} [1 0] {} [0 1] {} [1 1] {}}))

