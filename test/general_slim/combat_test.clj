(ns general-slim.combat-test
  (:require [clojure.test :refer [deftest is are]]
            [general-slim.combat :as SUT]))

(deftest trivial-cases
  (is (SUT/resolve-combat {:soldiers 100 :terrain :field}
                          {:soldiers 100 :terrain :field}))
  (are [unit] (thrown? Exception (SUT/resolve-combat unit unit))
    {}
    {:soldiers 100}
    {:soldiers 100 :terrain :fake-terrain}
    {:soldiers 0 :terrain :field}
    {:soldiers -5 :terrain :field}))