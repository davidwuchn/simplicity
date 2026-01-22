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

;; TTL for game sessions: 1 hour (in milliseconds)
(def ^:private game-ttl-ms (* 60 60 1000))

;; Game state structure:
;; {:board #{[x y] ...} :generation n :created-at timestamp :last-accessed timestamp}

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
         bounded-board (into #{} (filter in-bounds?) board)
         now (System/currentTimeMillis)]
     (swap! games assoc game-id
            {:board bounded-board
             :generation 0
             :created-at now
             :last-accessed now})
     bounded-board)))

(defn evolve!
  [game-id]
  (let [result (atom nil)
        now (System/currentTimeMillis)]
    (swap! games
           (fn [games-map]
             (if-let [game (get games-map game-id)]
               (let [new-board (next-generation (:board game))
                     new-gen (inc (:generation game))]
                 (reset! result new-board)
                 (assoc games-map game-id
                        (assoc game 
                               :board new-board 
                               :generation new-gen
                               :last-accessed now)))
               games-map)))
    @result))

(defn clear-cells!
  [game-id cells]
  (let [cells-set (set cells)
        result (atom nil)]
    (swap! games
           (fn [games-map]
             (if-let [game (get games-map game-id)]
               (let [new-board (set/difference (:board game) cells-set)]
                 (reset! result new-board)
                 (assoc-in games-map [game-id :board] new-board))
               games-map)))
    @result))

(defn add-cells!
  [game-id cells]
  (let [bounded-cells (into #{} (filter in-bounds?) cells)
        result (atom nil)]
    (swap! games
           (fn [games-map]
             (if-let [game (get games-map game-id)]
               (let [new-board (set/union (:board game) bounded-cells)]
                 (reset! result new-board)
                 (assoc-in games-map [game-id :board] new-board))
               games-map)))
    @result))

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
;; Musical Triggers (∃ Truth - deterministic mapping)
;; ------------------------------------------------------------

(defn- board-density
  [board]
  (let [cell-count (count board)]
    (min 1.0 (/ cell-count 100.0))))

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

(defn cleanup-stale-games!
  "Remove games that haven't been accessed within TTL.
   Returns the number of games removed."
  []
  (let [now (System/currentTimeMillis)
        cutoff (- now game-ttl-ms)
        removed (atom 0)]
    (swap! games
           (fn [games-map]
             (let [active-games (into {}
                                      (filter (fn [[_ game]]
                                                (> (:last-accessed game 0) cutoff)))
                                      games-map)]
               (reset! removed (- (count games-map) (count active-games)))
               active-games)))
    @removed))

(defonce ^:private cleanup-executor (atom nil))

(defn start-cleanup-scheduler!
  "Start background cleanup of stale games every 10 minutes."
  []
  (when-not @cleanup-executor
    (let [executor (java.util.concurrent.Executors/newSingleThreadScheduledExecutor)]
      (.scheduleAtFixedRate executor
                            (fn [] 
                              (try
                                (let [removed (cleanup-stale-games!)]
                                  (when (pos? removed)
                                    (println "Game cleanup: removed" removed "stale games")))
                                (catch Exception e
                                  (println "Game cleanup error:" (.getMessage e)))))
                            10  ;; initial delay
                            10  ;; period
                            java.util.concurrent.TimeUnit/MINUTES)
      (reset! cleanup-executor executor))))

(defn stop-cleanup-scheduler!
  "Stop the background cleanup scheduler."
  []
  (when-let [executor @cleanup-executor]
    (.shutdown executor)
    (reset! cleanup-executor nil)))

(defn initialize!
  []
  ;; Reset state on initialization
  (reset! games {})
  (reset! saved-games {})
  ;; Start cleanup scheduler
  (start-cleanup-scheduler!))
