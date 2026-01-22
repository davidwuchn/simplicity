(ns cc.mindward.game.interface-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cc.mindward.game.interface :as game]))

(use-fixtures :each
  (fn [test-fn]
    (game/initialize!)
    (test-fn)))

(deftest game-creation-test
  (testing "create game with nil initial board"
    (let [board (game/create-game! :test-nil nil)]
      (is (set? board) "board must be a set")
      (is (= 0 (count board)) "nil creates empty board")))
  
  (testing "create game with empty set"
    (let [board (game/create-game! :test-empty #{})]
      (is (= 0 (count board)) "empty set creates empty board")))
  
  (testing "create game with custom initial state"
    (let [initial #{[0 0] [1 0] [2 0]}  ; Horizontal line (blinker)
          board (game/create-game! :test-custom initial)]
      (is (= initial board) "board should match initial state"))))

(deftest get-board-test
  (testing "get-board returns current board state"
    (let [initial #{[0 0] [1 1]}
          created (game/create-game! :get-board-test initial)
          retrieved (game/get-board :get-board-test)]
      (is (= initial retrieved) "retrieved board should match created board")))
  
  (testing "get-board with non-existent game-id"
    (is (nil? (game/get-board :non-existent)) "returns nil for missing game-id")))

(deftest get-generation-test
  (testing "get-generation returns correct generation count"
    (game/create-game! :gen-query-test)
    (is (= 0 (game/get-generation :gen-query-test)) "starts at generation 0")
    (game/evolve! :gen-query-test)
    (is (= 1 (game/get-generation :gen-query-test)) "increments after evolution"))
  
  (testing "get-generation with non-existent game-id"
    (is (= 0 (game/get-generation :non-existent)) "returns 0 default for missing game-id")))

(deftest initialization-test
  (testing "initialize! resets game state"
    (game/create-game! :pre-init #{[0 0]})
    (is (some? (game/get-board :pre-init)) "game exists before initialize")
    (game/initialize!)
    (is (nil? (game/get-board :pre-init)) "initialize clears all games")))

(deftest evolution-test
  (testing "blinker oscillator evolves correctly"
    (let [blinker-vertical #{[1 0] [1 1] [1 2]}
          _ (game/create-game! :blinker-perfect blinker-vertical)
          evolved1 (game/evolve! :blinker-perfect)
          evolved2 (game/evolve! :blinker-perfect)]
      ;; Phase 1: should become horizontal
      (is (= #{[0 1] [1 1] [2 1]} evolved1) "blinker phase 1: vertical → horizontal")
      ;; Phase 2: should return to vertical (period-2 oscillator)
      (is (= #{[1 0] [1 1] [1 2]} evolved2) "blinker phase 2: horizontal → vertical (period-2)")))
  
  (testing "block pattern is stable (still life)"
    (let [block #{[0 0] [0 1] [1 0] [1 1]}
          _ (game/create-game! :block-stable block)]
      (dotimes [n 5]
        (is (= block (game/evolve! :block-stable)) (str "block should be stable at gen " n)))))
  
  (testing "generation counter increments correctly"
    (game/create-game! :gen-evolve-test)
    (is (= 0 (game/get-generation :gen-evolve-test)) "initial generation 0")
    (game/evolve! :gen-evolve-test)
    (is (= 1 (game/get-generation :gen-evolve-test)) "after 1 evolution")
    (game/evolve! :gen-evolve-test)
    (is (= 2 (game/get-generation :gen-evolve-test)) "after 2 evolutions")))

(deftest cellular-automata-rules-test
  (testing "Rule 1: any live cell with 2-3 neighbors lives"
    (let [board #{[0 0] [1 0] [2 0]}  ; Horizontal line
          _ (game/create-game! :rule1-test board)
          evolved (game/evolve! :rule1-test)]
      (is (= #{[1 -1] [1 0] [1 1]} evolved) "middle cell survives, ends die")))
  
  (testing "Rule 2: any dead cell with exactly 3 neighbors becomes alive"
    (let [board #{[0 0] [1 0] [0 1]}  ; L-shape, missing [1 1]
          _ (game/create-game! :rule2-test board)
          evolved (game/evolve! :rule2-test)]
      (is (contains? evolved [1 1]) "dead cell at [1 1] should become alive")
      (is (>= (count evolved) 4) "should have at least 4 cells after birth")))
  
  (testing "Rule 3: all other cells die (loneliness or overpopulation)"
    (let [single-cell #{[0 0]}
          overpop #{[0 0] [1 0] [0 1] [1 1] [2 2]}  ; Too dense
          _ (game/create-game! :rule3-lonely single-cell)
          __ (game/create-game! :rule3-overpop overpop)
          lonely-evolved (game/evolve! :rule3-lonely)
          overpop-evolved (game/evolve! :rule3-overpop)]
      (is (= 0 (count lonely-evolved)) "isolated cell dies from loneliness")
      (is (= 0 (count overpop-evolved)) "dense cluster dies from overpopulation"))))

(deftest cell-manipulation-test
  (testing "add cells to existing board"
    (game/create-game! :add-test #{[0 0]})
    (let [new-board (game/add-cells! :add-test #{[5 5] [10 10]})]
      (is (contains? new-board [0 0]) "original cell preserved")
      (is (contains? new-board [5 5]) "new cells added")
      (is (contains? new-board [10 10]))
      (is (= 3 (count new-board)))))
  
  (testing "add cells to empty board"
    (game/create-game! :add-empty)
    (let [new-board (game/add-cells! :add-empty #{[1 1] [2 2]})]
      (is (= 2 (count new-board)) "adds to empty board")
      (is (contains? new-board [1 1]))
      (is (contains? new-board [2 2]))))
  
  (testing "clear cells from board"
    (game/create-game! :clear-test #{[0 0] [1 1] [2 2]})
    (let [new-board (game/clear-cells! :clear-test #{[1 1]})]
      (is (contains? new-board [0 0]) "unaffected cells preserved")
      (is (not (contains? new-board [1 1])) "specified cells removed")
      (is (contains? new-board [2 2]))
      (is (= 2 (count new-board)))))
  
  (testing "clear non-existent cells has no effect"
    (game/create-game! :clear-nonexist #{[0 0]})
    (let [new-board (game/clear-cells! :clear-nonexist #{[99 99]})]
      (is (= #{[0 0]} new-board) "board unchanged when clearing non-existent cells")))
  
  (testing "bounds checking prevents out-of-range cells"
    (game/create-game! :bounds-test)
    (let [bounded (game/add-cells! :bounds-test #{[200 200] [-200 -200] [5 5] [-5 99]})]
      (is (= 1 (count bounded)) "filters out-of-bounds cells")
      (is (contains? bounded [5 5]) "only valid cells remain")
      (is (not (contains? bounded [200 200])) "exceeds max bounds ignored")
      (is (not (contains? bounded [-200 -200])) "below min bounds ignored")))
  
  (testing "manipulation on non-existent game returns nil"
    (is (nil? (game/add-cells! :nonexistent-game #{[0 0]})) "add to nil game")
    (is (nil? (game/clear-cells! :nonexistent-game #{[0 0]})) "clear from nil game")))

(deftest scoring-test
  (testing "score increases with cell count and generation"
    (game/create-game! :score-increase #{[0 0] [1 0] [2 0]})
    (let [score-0 (game/get-score :score-increase)
          _ (is (pos? score-0) "initial score positive")
          _ (game/evolve! :score-increase)
          score-1 (game/get-score :score-increase)]
      (is (> score-1 score-0) "score increases after evolution")))
  
  (testing "stability bonus for sustained life"
    (game/create-game! :stability #{[0 0] [0 1] [1 0] [1 1]}) ; Stable block
    (dotimes [_ 11] (game/evolve! :stability))
    (let [score (game/get-score :stability)]
      (is (>= score 100) "stability bonus after 10+ generations")))
  
  (testing "score with empty board"
    (game/create-game! :score-empty #{})  
    (let [score (game/get-score :score-empty)]
      (is (= 1 score) "empty board has base score of 1")))
  
  (testing "get-score with non-existent game"
    (is (nil? (game/get-score :non-existent-score)) "returns nil for missing game")))

(deftest pattern-analysis-test
  (testing "detect single block pattern"
    (game/create-game! :block-analyze #{[0 0] [0 1] [1 0] [1 1]})
    (let [analysis (game/get-pattern-analysis :block-analyze)]
      (is (= 1 (get-in analysis [:block :count])) "detects one block")
      (is (some #{[0 0]} (get-in analysis [:block :locations])) "finds block location")))
  
  (testing "detect multiple patterns"
    (game/create-game! :multi-pattern #{[0 0] [0 1] [1 0] [1 1]  ; block
                                                                   [5 5] [5 6] [5 7]})  ; vertical blinker
    (let [analysis (game/get-pattern-analysis :multi-pattern)]
      (is (= 1 (get-in analysis [:block :count])) "detects block")
      (is (= 1 (get-in analysis [:blinker :count])) "detects blinker")))
  
  (testing "detect beehive pattern"
    (game/create-game! :beehive-pattern #{[0 1] [0 2] [1 0] [1 3] [2 1] [2 2]})
    (let [analysis (game/get-pattern-analysis :beehive-pattern)]
      (is (= 1 (get-in analysis [:beehive :count])) "detects beehive patter")))
  
  (testing "detect glider pattern"
    (game/create-game! :glider-pattern #{[0 1] [1 2] [2 0] [2 1] [2 2]})
    (let [analysis (game/get-pattern-analysis :glider-pattern)]
      (is (= 1 (get-in analysis [:glider :count])) "detects glider pattern")))
  
  (testing "pattern analysis with no patterns"
    (game/create-game! :no-patterns #{[0 0] [5 5] [10 10]})  ; Isolated cells, not patterns
    (let [analysis (game/get-pattern-analysis :no-patterns)
          counts (mapv #(get-in analysis [% :count]) [:block :beehive :blinker :toad :glider])]
      (is (every? zero? counts) "no patterns detected in isolated cells"))))

(deftest pattern-analysis-test
  (testing "detect block pattern"
    (game/create-game! :block-analyze #{[0 0] [0 1] [1 0] [1 1]})
    (let [analysis (game/get-pattern-analysis :block-analyze)]
      (is (= 1 (get-in analysis [:block :count])) "should detect one block")
      (is (some #{[0 0]} (get-in analysis [:block :locations]))))))

(deftest musical-triggers-test
  (testing "generate triggers based on board state"
    (game/create-game! :music-1 #{[0 0] [1 0] [2 0] [3 0] [4 0]})
    (let [triggers (game/get-musical-triggers :music-1)]
      (is (vector? triggers))
      (is (pos? (count triggers)))
      (is (every? :trigger triggers) "each trigger should have :trigger key")
      (is (every? :params triggers) "each trigger should have :params key"))))

(deftest musical-triggers-density-test
  (testing "high density board triggers density-high"
    (let [high-density (set (for [x (range 8) y (range 8)] [x y]))
          _ (game/create-game! :music-high high-density)
          triggers (game/get-musical-triggers :music-high)
          has-density-high? (some #(= :density-high (:trigger %)) triggers)]
      (is (vector? triggers) "returns vector")
      (is (pos? (count triggers)) "has triggers")
      (is has-density-high? "density-high trigger present for >50 cells")))
  
  (testing "medium density board triggers density-mid"
    (let [mid-density (set (for [x (range 5) y (range 5)] [x y]))  ; 25 cells
          _ (game/create-game! :music-mid mid-density)
          triggers (game/get-musical-triggers :music-mid)
          has-density-mid? (some #(= :density-mid (:trigger %)) triggers)]
      (is has-density-mid? "density-mid trigger for >20 cells")))
  
  (testing "empty board still generates drone trigger"
    (game/create-game! :music-empty #{})
    (let [triggers (game/get-musical-triggers :music-empty)
          has-drone? (some #(= :drone (:trigger %)) triggers)]
      (is (seq triggers) "empty board has triggers")
      (is has-drone? "drone trigger always present")))
  
  (testing "musical triggers format validation"
    (game/create-game! :music-format #{[0 0] [1 0] [2 0]})
    (let [triggers (game/get-musical-triggers :music-format)]
      (is (vector? triggers), "triggers is a vector")
      (is (every? :trigger triggers), "each trigger has :trigger key")
      (is (every? :params triggers), "each trigger has :params key")
      (is (every? map? triggers), "each trigger is a map")
      (is (every? #(keyword? (:trigger %)) triggers), ":trigger values are keywords")))
  
  (testing "get-musical-triggers with non-existent game"
    (is (nil? (game/get-musical-triggers :non-existent-music)) "returns nil for missing game")))

(deftest persistence-test
  (testing "save and load game state preserves board"
    (game/initialize!)
    (game/create-game! :save-test #{[0 0] [1 1] [2 2]})
    (let [saved (game/save-game! :save-test "test-game")
          loaded-id :loaded-test
          loaded-board (game/load-game! (:id saved) loaded-id)]
      (is (= #{[0 0] [1 1] [2 2]} loaded-board) "preserves exact board state")
      (is (= 1 (count (game/list-saved-games))) "saved game in list")))
  
  (testing "list saved games shows complete metadata"
    (let [games (game/list-saved-games)]
      (is (vector? games) "returns vector")
      (is (= 1 (count games)), "correct count")
      (let [game-meta (first games)]
        (is (contains? game-meta :id), "has :id")
        (is (string? (:id game-meta)), ":id is string")
        (is (contains? game-meta :name), "has :name")
        (is (= "test-game" (:name game-meta)), "preserves name")
        (is (contains? game-meta :generation), "has :generation")
        (is (number? (:generation game-meta)), ":generation is number")
        (is (contains? game-meta :score), "has :score")
        (is (number? (:score game-meta)), ":score is number"))))
  
  (testing "save-game! preserves generation and calculates score"
    (game/create-game! :save-gen-test #{[0 0]})
    (dotimes [_ 5] (game/evolve! :save-gen-test))
    (let [saved (game/save-game! :save-gen-test "with-gen")
          saved-meta (first (filter #(= (:id %) (:id saved)) (game/list-saved-games)))]
      (is (= 5 (:generation saved)) "preserves generation count")
      (is (pos? (:score saved-meta)) "calculates score")))
  
  (testing "delete saved game removes from list"
    (game/initialize!)
    (game/create-game! :delete-test #{[0 0]})
    (game/save-game! :delete-test "to-delete")
    (let [saved-id (:id (first (game/list-saved-games)))
          initial-count (count (game/list-saved-games))]
      (game/delete-game! saved-id)
      (is (= (dec initial-count) (count (game/list-saved-games))) "decremented saved games count")
      (is (empty? (filter #(= (:id %) saved-id) (game/list-saved-games))) "deleted game removed")))
  
  (testing "load-game! with non-existent saved-id"
    (is (nil? (game/load-game! "fake-uuid" :should-fail)) "returns nil for missing saved game"))
  
  (testing "delete-game! with non-existent id is safe"
    (game/delete-game! "fake-uuid") "no-op for non-existent saved game")
  
  (testing "multiple games saved and independent"
    (game/initialize!)
    (game/create-game! :multi-1 #{[0 0]})
    (game/evolve! :multi-1)
    (game/save-game! :multi-1 "game-1")
    
    (game/create-game! :multi-2 #{[5 5]})
    (game/save-game! :multi-2 "game-2")
    
    (is (= 2 (count (game/list-saved-games))) "two games saved")
    (let [games (game/list-saved-games)]
      (is (= 1 (get-in (first games) [:generation])) "first game evolved 1 time")
      (is (= 0 (get-in (second games) [:generation])) "second game at generation 0"))))

(deftest boundary-conditions-test
  (testing "evolution maintains reasonable bounds"
    (let [near-edge #{[90 0] [90 1] [90 2]}
          _ (game/create-game! :edge-evolve near-edge)
          evolved (game/evolve! :edge-evolve)]
      (is (>= 100 (apply max (map first evolved))) "evolved cells within reasonable bounds")
      (is (<= -100 (apply min (map first evolved))) "evolved cells within reasonable bounds"))))

(deftest error-handling-and-stability-test
  (testing "all operations with nil game-id handled gracefully"
    (is (nil? (game/get-board nil)) "get-board nil")
    (is (nil? (game/get-generation nil)) "get-generation nil returns 0")
    (is (nil? (game/get-score nil)) "get-score nil")
    (is (nil? (game/evolve! nil)) "evolve! nil")
    (is (nil? (game/add-cells! nil #{[0 0]})) "add-cells! nil game")
    (is (nil? (game/clear-cells! nil #{[0 0]})) "clear-cells! nil game")
    (is (nil? (game/save-game! nil "name")) "save-game! nil game")
    (is (nil? (game/get-musical-triggers nil)) "get-musical-triggers nil")
    (is (nil? (game/get-pattern-analysis nil)) "get-pattern-analysis nil"))
  
  (testing "evolution stability - known patterns reasonable bounds"
    (let [patterns [#{[0 0] [0 1] [1 0] [1 1]}  ; block
                    #{[0 0] [0 1] [0 2]}]  ; vertical blinker
          game-ids (mapv #(keyword (str "stable-" %)) (range (count patterns)))]
      (doseq [[game-id pattern] (map vector game-ids patterns)]
        (game/create-game! game-id pattern)
        (dotimes [n 10]
          (game/evolve! game-id)
          (let [board (game/get-board game-id)]
            (is (vector? (seq board)) "board rendered properly")
            (is (<= 10 (count board) 20) "reasonable pattern evolution")))))))
  
  (testing "load-game! creates new independent game"
    (game/initialize!)
    (game/create-game! :original #{[0 0]})
    (let [saved (game/save-game! :original "independent")
          _ (game/load-game! (:id saved) :loaded)
          evolved-original (game/evolve! :original)
          evolved-loaded (game/get-board :loaded)]
      (is (not= evolved-original evolved-loaded) "original evolution does not affect loaded game")
      (is (= 0 (game/get-generation :loaded)) "loaded game starts at generation 0")))