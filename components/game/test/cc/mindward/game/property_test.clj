(ns cc.mindward.game.property-test
  "Property-based tests for game component using test.check.

   Tests Game of Life properties:
   - Determinism: Same board always evolves to same next state
   - Idempotence: Dead board stays dead
   - Conservation: Certain patterns are stable (still lifes)
   - Periodicity: Certain patterns repeat (oscillators)

   φ (Vitality): Generative testing explores edge cases organically
   ∃ (Truth): Verifies mathematical properties of Conway's Game of Life"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [cc.mindward.game.interface :as game]))

;; ============================================================================
;; Generators for Game of Life boards
;; ============================================================================

(def coordinate-gen
  "Generator for board coordinates within reasonable bounds"
  (gen/tuple (gen/choose -10 10) (gen/choose -10 10)))

(def board-gen
  "Generator for Game of Life boards (sets of coordinates)"
  (gen/set coordinate-gen {:max-elements 50}))

(def small-board-gen
  "Generator for small boards (for performance)"
  (gen/set coordinate-gen {:max-elements 20}))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(defn game-fixture
  "Fixture that initializes game state before each test."
  [f]
  (game/initialize!)
  (f))

(use-fixtures :each game-fixture)

;; ============================================================================
;; Property: Determinism
;; ============================================================================

(def determinism-prop
  "Property: Game evolution is deterministic.
   Same board should always evolve to same next state."
  (prop/for-all [board board-gen]
                (let [game-id (keyword (str "determinism-test-" (hash board)))
                      _ (game/create-game! game-id board)
                      result1 (game/evolve! game-id)
                      _ (game/create-game! game-id board) ; Reset to same board
                      result2 (game/evolve! game-id)]
                  (= result1 result2))))

(defspec determinism-property 50
  determinism-prop)

;; ============================================================================
;; Property: Idempotence of Dead Board
;; ============================================================================

(def dead-board-idempotent-prop
  "Property: Dead board (empty) stays dead after evolution."
  (prop/for-all [_ gen/int] ; Board is always empty, so we don't need board input
                (let [game-id (keyword (str "dead-test-" (rand-int 1000000)))
                      _ (game/create-game! game-id #{})
                      after-evolve (game/evolve! game-id)]
                  (empty? after-evolve))))

(defspec dead-board-idempotent 50
  dead-board-idempotent-prop)

;; ============================================================================
;; Property: Still Life Stability
;; ============================================================================

(def still-life-patterns
  "Known still life patterns in Conway's Game of Life.
   These should remain unchanged after evolution."
  {:block #{[0 0] [0 1] [1 0] [1 1]}
   :beehive #{[0 1] [0 2] [1 0] [1 3] [2 1] [2 2]}
   :loaf #{[0 1] [0 2] [1 0] [1 3] [2 1] [2 3] [3 2]}})

(deftest still-life-stability-test
  (testing "Still life patterns remain unchanged after evolution"
    (doseq [[pattern-name cells] still-life-patterns]
      (let [game-id (keyword (str "still-" (name pattern-name)))
            _ (game/create-game! game-id cells)
            after-evolve (game/evolve! game-id)]
        (is (= cells after-evolve)
            (str pattern-name " should be stable"))))))

;; ============================================================================
;; Property: Oscillator Periodicity
;; ============================================================================

(def blinker-pattern
  "Blinker oscillator (period 2)"
  #{[0 0] [1 0] [2 0]})

(def blinker-periodicity-prop
  "Property: Blinker oscillator returns to original state after 2 evolutions."
  (prop/for-all [_ gen/int] ; Pattern is fixed, so we don't need input
                (let [game-id (keyword (str "blinker-test-" (rand-int 1000000)))
                      _ (game/create-game! game-id blinker-pattern)
                      after-1 (game/evolve! game-id)
                      after-2 (game/evolve! game-id)]
                  (= blinker-pattern after-2))))

(defspec blinker-periodicity 20
  blinker-periodicity-prop)

;; ============================================================================
;; Property: Board Size Bounds
;; ============================================================================

(def board-size-reasonable-prop
  "Property: Evolved board size stays within reasonable bounds."
  (prop/for-all [board small-board-gen]
                (let [game-id (keyword (str "size-test-" (hash board)))
                      _ (game/create-game! game-id board)
                      evolved (game/evolve! game-id)]
      ;; Most patterns don't grow beyond 4x original size in one step
                  (<= (count evolved) (* 4 (max 1 (count board)))))))

(defspec board-size-reasonable 50
  board-size-reasonable-prop)

;; ============================================================================
;; Property: Coordinate Integrity
;; ============================================================================

(def coordinates-remain-integers-prop
  "Property: All coordinates remain integers after evolution."
  (prop/for-all [board board-gen]
                (let [game-id (keyword (str "coord-test-" (hash board)))
                      _ (game/create-game! game-id board)
                      evolved (game/evolve! game-id)]
                  (every? (fn [[x y]]
                            (and (integer? x) (integer? y)))
                          evolved))))

(defspec coordinates-remain-integers 50
  coordinates-remain-integers-prop)

;; ============================================================================
;; Integration: Property tests work with real game functions
;; ============================================================================

(deftest property-tests-integration
  (testing "Property tests integrate with actual game interface"
    (testing "determinism with real game state"
      (let [board #{[0 0] [1 1] [2 2]}
            game-id :integration-test
            _ (game/create-game! game-id board)
            gen1 (game/get-generation game-id)
            _ (game/evolve! game-id)
            gen2 (game/get-generation game-id)]
        (is (= 0 gen1) "starts at generation 0")
        (is (= 1 gen2) "increments after evolution")))))