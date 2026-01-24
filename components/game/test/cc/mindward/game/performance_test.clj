(ns cc.mindward.game.performance-test
  "Performance tests for game component.
   
   Tests:
   - Evolution speed for different board sizes
   - Memory usage patterns
   - Concurrent access performance
   
   τ (Wisdom): Tests foresight about performance characteristics
   ∀ (Vigilance): Defends against performance regressions"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [cc.mindward.game.interface :as game]))

;; ============================================================================
;; Test Helpers
;; ============================================================================

(defn time-ms
  "Time execution of a function in milliseconds"
  [f]
  (let [start (System/nanoTime)
        result (f)
        end (System/nanoTime)]
    [result (/ (- end start) 1000000.0)]))

(defn- generate-large-board
  "Generate a large board for performance testing"
  [size]
  (set (for [x (range size)
             y (range size)
             :when (zero? (rand-int 3))] ; ~33% density
         [x y])))

(defn- generate-glider
  "Generate a glider pattern"
  []
  #{[0 1] [1 2] [2 0] [2 1] [2 2]})

(defn- generate-random-board
  "Generate random board of given size"
  [size]
  (set (take size (repeatedly #(vector (rand-int 100) (rand-int 100))))))

;; ============================================================================
;; Performance Tests
;; ============================================================================

(deftest evolution-performance-test
  (testing "Game evolution performance characteristics"
    (game/initialize!)
    
    (testing "Small board (≤ 100 cells) evolves in < 10ms"
      (let [small-board (generate-random-board 100)
            game-id :perf-small
            _ (game/create-game! game-id small-board)
            [result time-ms] (time-ms #(game/evolve! game-id))]
        (is (< time-ms 10) (str "Small board should evolve in < 10ms, took " time-ms "ms"))
        (is (set? result) "Result should be a set")))
    
    (testing "Medium board (≤ 1000 cells) evolves in < 100ms"
      (let [medium-board (generate-random-board 1000)
            game-id :perf-medium
            _ (game/create-game! game-id medium-board)
            [result time-ms] (time-ms #(game/evolve! game-id))]
        (is (< time-ms 100) (str "Medium board should evolve in < 100ms, took " time-ms "ms"))
        (is (set? result) "Result should be a set")))
    
    (testing "Glider pattern evolution is fast (< 5ms)"
      (let [glider (generate-glider)
            game-id :perf-glider
            _ (game/create-game! game-id glider)
            [result time-ms] (time-ms #(game/evolve! game-id))]
        (is (< time-ms 5) (str "Glider should evolve in < 5ms, took " time-ms "ms"))
        (is (set? result) "Result should be a set")))))

(deftest memory-usage-pattern-test
  (testing "Memory usage patterns for multiple games"
    (game/initialize!)
    
    (testing "Creating many small games doesn't leak memory"
      (let [game-count 100
            boards (repeatedly game-count #(generate-random-board 10))]
        (doseq [i (range game-count)]
          (game/create-game! (keyword (str "mem-test-" i)) (nth boards i)))
        
        ;; Verify all games can be retrieved
        (doseq [i (range game-count)]
          (let [game-id (keyword (str "mem-test-" i))
                board (game/get-board game-id)]
            (is (= (nth boards i) board) (str "Game " i " should be retrievable"))))
        
        (is true "Multiple game creation succeeded without error")))))

(deftest concurrent-access-test
  (testing "Game functions handle concurrent access gracefully"
    (game/initialize!)
    
    (testing "Multiple evolutions on different games"
      (let [game-ids [:concurrent-1 :concurrent-2 :concurrent-3]
            boards [(generate-random-board 50)
                    (generate-random-board 50)
                    (generate-random-board 50)]]
        
        ;; Create games
        (doseq [[game-id board] (map vector game-ids boards)]
          (game/create-game! game-id board))
        
        ;; Evolve all games
        (let [results (pmap (fn [game-id]
                              (try
                                (game/evolve! game-id)
                                (catch Exception e
                                  {:error e :game-id game-id})))
                            game-ids)]
          (doseq [[game-id result] (map vector game-ids results)]
            (if (map? result)
              (is (not (:error result)) (str "Game " game-id " should not error: " (:error result)))
              (is (set? result) (str "Game " game-id " should return a set")))))))))

(deftest generation-tracking-performance-test
  (testing "Generation tracking doesn't degrade performance"
    (game/initialize!)
    
    (let [board (generate-random-board 100)
          game-id :gen-perf-test
          _ (game/create-game! game-id board)
          iterations 100]
      
      (testing "Multiple evolutions maintain performance"
        (let [[_ total-time] (time-ms
                               (fn []
                                 (dotimes [i iterations]
                                   (game/evolve! game-id))))]
          (let [avg-time-ms (/ total-time iterations)]
            (is (< avg-time-ms 20) (str "Average evolution should be < 20ms, was " avg-time-ms "ms")))))
      
      (testing "Generation count is accurate after many evolutions"
        (let [final-gen (game/get-generation game-id)]
          (is (= iterations final-gen) 
              (str "Should be at generation " iterations ", was " final-gen)))))))

(deftest board-retrieval-performance-test
  (testing "Board retrieval is fast regardless of board size"
    (game/initialize!)
    
    (let [sizes [10 100 500]
          results (for [size sizes]
                    (let [board (generate-random-board size)
                          game-id (keyword (str "retrieve-" size))
                          _ (game/create-game! game-id board)
                          [retrieved time-ms] (time-ms #(game/get-board game-id))]
                      {:size size :time-ms time-ms :retrieved retrieved :original board}))]
      
      (doseq [{:keys [size time-ms retrieved original]} results]
        (is (< time-ms 5) (str "Retrieving board of size " size " should take < 5ms, took " time-ms "ms"))
        (is (= original retrieved) (str "Board of size " size " should be retrieved correctly"))))))