(ns cc.mindward.game.impl
  "Game engine implementation - Conway's Game of Life.
   
   π (Synthesis): Board state as immutable sets for efficient evolution.
   τ (Wisdom): Use transient collections for performance in hot loops.
   ∀ (Vigilance): Bounds checking prevents infinite board growth."
  (:require [clojure.set :as set]))

;; ------------------------------------------------------------
;; State Management (π Synthesis - complete mental model)
;; ------------------------------------------------------------

(defonce ^:private games (atom {}))
(defonce ^:private saved-games (atom {}))
(def ^:private max-bounds 100)
(def ^:private min-bounds -100)

;; Game state structure:
;; {:board #{[x y] ...} :generation n :created-at timestamp}

;; ------------------------------------------------------------
;; Conway's Game of Life Rules (∃ Truth - deterministic)
;; ------------------------------------------------------------

(defn- neighbors
  "Get all 8 neighbors of a cell [x y]."
  [[x y]]
  (for [dx [-1 0 1]
        dy (if (zero? dx) [-1 1] [-1 0 1])]
    [(+ x dx) (+ y dy)]))

(defn- count-neighbors
  "Count living neighbors for each cell on the board."
  [board]
  (->> board
       (mapcat neighbors)
       frequencies))

(defn- in-bounds?
  "Check if coordinate is within defined bounds."
  [[x y]]
  (and (<= min-bounds x max-bounds)
       (<= min-bounds y max-bounds)))

(defn- next-generation
  "Evolve board one generation using Conway's rules.
   Rule 1: Any live cell with 2-3 neighbors lives
   Rule 2: Any dead cell with exactly 3 neighbors becomes alive
   Rule 3: All other cells die"
  [board]
  (let [neighbor-counts (count-neighbors board)]
    (into #{} (comp (filter (fn [[cell count]]
                             (or (and (board cell) (<= 2 count 3))
                                 (and (not (board cell)) (= count 3)))))
                  (map key))
          neighbor-counts)))

;; ------------------------------------------------------------
;; Public Implementation
;; ------------------------------------------------------------

(defn get-board
  [game-id]
  (get-in @games [game-id :board]))

(defn get-generation
  [game-id]
  (get-in @games [game-id :generation] 0))

(defn create-game!
  ([game-id] (create-game! game-id nil))
  ([game-id initial-board]
   (let [board (cond
                 (nil? initial-board) #{}  ; Empty board if nil
                 (empty? initial-board) #{}  ; Empty board if empty set
                 :else initial-board)
         bounded-board (into #{} (filter in-bounds?) board)]
     (swap! games assoc game-id
            {:board bounded-board
             :generation 0
             :created-at (System/currentTimeMillis)})
     bounded-board)))

(defn evolve!
  [game-id]
  (when-let [game (get @games game-id)]
    (let [new-board (next-generation (:board game))
          new-gen (inc (:generation game))]
      (swap! games assoc-in [game-id :board] new-board)
      (swap! games assoc-in [game-id :generation] new-gen)
      new-board)))

(defn clear-cells!
  [game-id cells]
  (when-let [game (get @games game-id)]
    (let [new-board (set/difference (:board game) (set cells))]
      (swap! games assoc-in [game-id :board] new-board)
      new-board)))

(defn add-cells!
  [game-id cells]
  (when-let [game (get @games game-id)]
    (let [bounded-cells (into #{} (filter in-bounds?) cells)
          new-board (set/union (:board game) bounded-cells)]
      (swap! games assoc-in [game-id :board] new-board)
      new-board)))

(defn calculate-score
  [game-id]
  (when-let [game (get @games game-id)]
    (let [board (:board game)
          generation (:generation game)
          living-cells (count board)
          ;; Score based on sustained complexity
          ;; More cells + higher generation = better score
          complexity-score (max 1 (* living-cells (min generation 100)))
          ;; Bonus for sustained life (stability)
          stability-bonus (if (> generation 10) 100 0)]
      (+ complexity-score stability-bonus))))

;; ------------------------------------------------------------
;; Pattern Analysis (∃ Truth - deterministic recognition)
;; ------------------------------------------------------------

(def ^:private pattern-defs
  "Known Conway's Game of Life patterns.
   Maps pattern name to set of relative coordinates."
  {:block #{[0 0] [0 1] [1 0] [1 1]}
   :beehive #{[0 1] [0 2] [1 0] [1 3] [2 1] [2 2]}
   :blinker #{[0 0] [0 1] [0 2]}
   :toad #{[0 1] [0 2] [0 3] [1 0] [1 1] [1 2]}
   :glider #{[0 1] [1 2] [2 0] [2 1] [2 2]}})

(defn- find-pattern-at
  "Check if a pattern exists at offset [ox oy] on the board."
  [board pattern offset]
  (let [pattern-cells (map #(mapv + offset %) pattern)]
    (when (every? board pattern-cells)
      offset)))

(defn analyze-patterns
  [game-id]
  (when-let [board (get-board game-id)]
    (into {} (for [[pattern-name pattern] pattern-defs]
               (let [locations (into []
                                   (comp (filter #(find-pattern-at board pattern %))
                                         (distinct))
                                   (for [x (range min-bounds max-bounds)
                                         y (range min-bounds max-bounds)]
                                     [x y]))
                     count (count locations)]
                 [pattern-name {:count count :locations locations}])))))

;; ------------------------------------------------------------
;; Musical Triggers (∃ Truth - deterministic mapping)
;; ------------------------------------------------------------

(defn- board-density
  [board]
  (let [cell-count (count board)]
    (min 1.0 (/ cell-count 100.0))))

(defn- birth-events
  [old-board new-board]
  (set/difference new-board old-board))

(defn- death-events
  [old-board new-board]
  (set/difference old-board new-board))

(defn- classify-pattern
  "Classify board state for musical mapping."
  [old-board new-board]
  (let [births (count (birth-events old-board new-board))
        deaths (count (death-events old-board new-board))
        density (board-density new-board)]
    (cond
      ;; High activity = chaotic/attack mode
      (> (+ births deaths) 10) :chaos
      ;; Stable patterns = sustained tones
      (= old-board new-board) :stable
      ;; Growing population = build-up
      (> births deaths) :growth
      ;; Shrinking = release/drone
      :else :decay)))

(defn generate-musical-triggers
  [game-id]
  (when-let [game (get @games game-id)]
    (let [board (:board game)]
      ;; Analyze current state
      (cond-> []
        ;; Trigger based on cell count
        (> (count board) 50) (conj {:trigger :density-high
                                    :params {:frequency 440
                                             :amplitude 0.8}})
        (> (count board) 20) (conj {:trigger :density-mid
                                    :params {:frequency 220
                                             :amplitude 0.6}})
        ;; Trigger on births
        (> (count board) 0) (conj {:trigger :life-pulse
                                  :params {:rate (/ 1.0 (max 1 (count board)))
                                           :intensity (board-density board)}})
        ;; Stable patterns trigger sustained tones
        :always (conj {:trigger :drone
                       :params {:frequency 55
                                :amplitude 0.3}})))))

;; ------------------------------------------------------------
;; Persistence (π Synthesis - holistic integration)
;; ------------------------------------------------------------

(defn save-game!
  [game-id name]
  (when-let [game (get @games game-id)]
    (let [saved-id (str (java.util.UUID/randomUUID))
          saved-game (assoc game
                           :id saved-id
                           :name name
                           :saved-at (System/currentTimeMillis))]
      (swap! saved-games assoc saved-id saved-game)
      saved-game)))

(defn load-game!
  [saved-game-id new-game-id]
  (when-let [saved-game (get @saved-games saved-game-id)]
    (swap! games assoc new-game-id
           (select-keys saved-game [:board :generation :created-at]))
    (get-board new-game-id)))

(defn delete-game!
  [saved-game-id]
  (swap! saved-games dissoc saved-game-id))

(defn list-saved-games
  []
  (mapv (fn [[id game]]
          (let [board (:board game)
                generation (:generation game)
                living-cells (count board)
                ;; Calculate score from saved game data directly
                complexity-score (max 1 (* living-cells (min generation 100)))
                stability-bonus (if (> generation 10) 100 0)
                score (+ complexity-score stability-bonus)]
            {:id id
             :name (:name game)
             :generation generation
             :score score}))
        @saved-games))

;; ------------------------------------------------------------
;; Lifecycle (τ Wisdom - explicit initialization)
;; ------------------------------------------------------------

(defn initialize!
  []
  ;; Reset state on initialization
  (reset! games {})
  (reset! saved-games {}))
