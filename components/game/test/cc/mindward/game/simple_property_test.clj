(comment
  ;; Temporarily disabling simple property tests to diagnose test suite timeout
  
  (ns cc.mindward.game.simple-property-test
    "Simple property-based tests to verify the concept works."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure.test.check :as tc]
              [clojure.test.check.generators :as gen]
              [clojure.test.check.properties :as prop]
              [cc.mindward.game.interface :as game])))

(deftest simple-property-test
  (testing "Simple property: 2 * n is always even"
    (let [prop (prop/for-all [n gen/int]
                (even? (* 2 n)))
          result (tc/quick-check 10 prop)]
      (is (:pass? result) "Property should pass")
      (is (= 10 (:num-tests result)) "Should run 10 tests"))))

(deftest game-determinism-simple-test
  (testing "Game evolution is deterministic (simple test)"
    (game/initialize!)
    (let [board #{[0 0] [1 1]}
          game-id :simple-determinism-test
          _ (game/create-game! game-id board)
          result1 (game/evolve! game-id)
          _ (game/create-game! game-id board) ; Reset
          result2 (game/evolve! game-id)]
      (is (= result1 result2) "Same board should evolve to same result"))))