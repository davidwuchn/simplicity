(ns cc.mindward.game.impl
  "Game engine implementation - Conway's Game of Life.
   
   π (Synthesis): Board state as immutable sets for efficient evolution.
   τ (Wisdom): Use transient collections for performance in hot loops.
   ∀ (Vigilance): Bounds checking prevents infinite board growth."
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]))

;; ------------------------------------------------------------
;; Domain Constants & Schema (∃ Truth)
;; ------------------------------------------------------------

(def ^:private max-bounds 100)
(def ^:private min-bounds -100)
(def ^:private game-ttl-ms (* 60 60 1000))

;; Game state structure:
;; {:board #{[x y] ...} :generation n :created-at timestamp :last-accessed timestamp}

;; ------------------------------------------------------------
;; State Management (π Synthesis)
;; ------------------------------------------------------------

(defonce ^:private games (atom {}))
(defonce ^:private saved-games (atom {}))
(defonce ^:private cleanup-executor (atom nil))

;; ------------------------------------------------------------
;; Helpers (λ Pure Functions)
;; ------------------------------------------------------------

(defn- neighbors
  "Get all 8 neighbors of a cell [x y]."
  [[x y]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not (and (zero? dx) (zero? dy)))]
    [(+ x dx) (+ y dy)]))

(defn- in-bounds?
  "Check if coordinate is within defined bounds."
  [[x y]]
  (and (<= min-bounds x max-bounds)
       (<= min-bounds y max-bounds)))

(defn- count-neighbors
  "Count living neighbors for each cell on the board."
  [board]
  (frequencies (mapcat neighbors board)))

(defn- next-generation
  "Evolve board one generation using Conway's rules."
  [board]
  (let [neighbor-counts (count-neighbors board)]
    (set (for [[cell count] neighbor-counts
               :let [alive? (contains? board cell)]
               :when (or (= count 3)
                         (and alive? (= count 2)))]
           cell))))

(defn- calculate-game-score
  "Pure function to calculate score from game state."
  [{:keys [board generation]}]
  (let [living-cells (count board)
        complexity-score (max 1 (* living-cells (min generation 100)))
        stability-bonus (if (> generation 10) 100 0)]
    (+ complexity-score stability-bonus)))

;; ------------------------------------------------------------
;; Internal State Updates (τ Wisdom)
;; ------------------------------------------------------------

(defn- update-game!
  "Internal helper for updating game state in the atom."
  [game-id update-fn]
  (let [result (atom nil)]
    (swap! games (fn [m]
                   (if-let [game (get m game-id)]
                     (let [new-game (update-fn game)]
                       (reset! result new-game)
                       (assoc m game-id new-game))
                     m)))
    @result))

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
   (let [board (set (filter in-bounds? initial-board))
         now (System/currentTimeMillis)
         game {:board board
               :generation 0
               :created-at now
               :last-accessed now}]
     (swap! games assoc game-id game)
     board)))

(defn evolve!
  [game-id]
  (let [now (System/currentTimeMillis)
        new-game (update-game! game-id 
                               (fn [g] 
                                 (-> g
                                     (update :board next-generation)
                                     (update :generation inc)
                                     (assoc :last-accessed now))))]
    (:board new-game)))

(defn clear-cells!
  [game-id cells]
  (let [cells-set (set cells)
        new-game (update-game! game-id 
                               #(update % :board set/difference cells-set))]
    (:board new-game)))

(defn add-cells!
  [game-id cells]
  (let [bounded-cells (set (filter in-bounds? cells))
        new-game (update-game! game-id 
                               #(update % :board set/union bounded-cells))]
    (:board new-game)))

(defn calculate-score
  [game-id]
  (when-let [game (get @games game-id)]
    (calculate-game-score game)))

(defn generate-musical-triggers
  [game-id]
  (when-let [board (get-board game-id)]
    (let [cell-count (count board)
          density (min 1.0 (/ cell-count 100.0))]
      (cond-> []
        (> cell-count 50) (conj {:trigger :density-high
                                 :params {:frequency 440
                                          :amplitude 0.8}})
        (> cell-count 20) (conj {:trigger :density-mid
                                 :params {:frequency 220
                                          :amplitude 0.6}})
        (pos? cell-count) (conj {:trigger :life-pulse
                                 :params {:rate (/ 1.0 (max 1 cell-count))
                                          :intensity density}})
        :always (conj {:trigger :drone
                       :params {:frequency 55
                                :amplitude 0.3}})))))

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
    (let [game-state (select-keys saved-game [:board :generation :created-at])
          now (System/currentTimeMillis)]
      (swap! games assoc new-game-id (assoc game-state :last-accessed now))
      (:board game-state))))

(defn delete-game!
  [saved-game-id]
  (swap! saved-games dissoc saved-game-id))

(defn list-saved-games
  []
  (mapv (fn [[id game]]
          {:id id
           :name (:name game)
           :generation (:generation game)
           :score (calculate-game-score game)})
        @saved-games))

;; ------------------------------------------------------------
;; Lifecycle (τ Wisdom)
;; ------------------------------------------------------------

(defn cleanup-stale-games!
  []
  (let [now (System/currentTimeMillis)
        cutoff (- now game-ttl-ms)
        before (count @games)]
    (swap! games (fn [m]
                   (into {} (filter (fn [[_ g]] (> (:last-accessed g 0) cutoff))) m)))
    (let [removed (- before (count @games))]
      (when (pos? removed)
        (log/infof "Game cleanup: removed %d stale games" removed))
      removed)))

(defn start-cleanup-scheduler!
  []
  (when-not @cleanup-executor
    (let [executor (java.util.concurrent.Executors/newSingleThreadScheduledExecutor)
          task (fn [] (try (cleanup-stale-games!) (catch Exception e (log/error e "Cleanup failed"))))]
      (.scheduleAtFixedRate executor task 10 10 java.util.concurrent.TimeUnit/MINUTES)
      (reset! cleanup-executor executor))))

(defn stop-cleanup-scheduler!
  []
  (when-let [executor @cleanup-executor]
    (.shutdown executor)
    (reset! cleanup-executor nil)))

(defn initialize!
  []
  (reset! games {})
  (reset! saved-games {})
  (start-cleanup-scheduler!))
