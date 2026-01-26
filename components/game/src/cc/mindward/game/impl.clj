(ns cc.mindward.game.impl
  "Game engine implementation - Conway's Game of Life.

   π (Synthesis): Board state as immutable sets for efficient evolution.
   τ (Wisdom): Use transient collections for performance in hot loops.
   ∀ (Vigilance): Bounds checking prevents infinite board growth."
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [cc.mindward.game.config :as config])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]))

;; ------------------------------------------------------------
;; Domain Constants & Schema (∃ Truth)
;; ------------------------------------------------------------
;; All magic numbers moved to cc.mindward.game.config namespace

;; Game state structure:
;; {:board #{[x y] ...} :generation n :created-at timestamp :last-accessed timestamp}

;; ------------------------------------------------------------
;; Database Configuration (τ Wisdom - connection pooling)
;; ------------------------------------------------------------

(defn make-datasource
  "Create a pooled datasource using HikariCP for game persistence."
  ([db-path] (make-datasource db-path {:pool? true}))
  ([db-path {:keys [pool?] :or {pool? true}}]
   (if pool?
     (let [config (doto (HikariConfig.)
                    (.setJdbcUrl (str "jdbc:sqlite:" db-path))
                    (.setMaximumPoolSize 10)
                    (.setMinimumIdle 2)
                    (.setConnectionTimeout 30000)
                    (.setIdleTimeout 600000)
                    (.setMaxLifetime 1800000)
                    (.setPoolName "simplicity-game-pool"))]
       (HikariDataSource. config))
     (jdbc/get-datasource {:dbtype "sqlite" :dbname db-path}))))

(defonce ^:private db-state (atom nil))

(defn get-datasource
  "Get or create datasource for saved games. Lazy initialization."
  []
  (or @db-state
      (let [db-path (or (System/getenv "DB_PATH") "simplicity.db")
            ds (make-datasource db-path)]
        (reset! db-state ds)
        ds)))

(def ^:dynamic *ds* nil)

(defn ds
  "Get current datasource. Uses *ds* if bound (for tests), otherwise get-datasource."
  []
  (or *ds* (get-datasource)))

(defn init-db!
  "Initialize saved_games table. Idempotent - safe to call multiple times."
  ([] (init-db! (ds)))
  ([datasource]
   (jdbc/execute! datasource ["
    CREATE TABLE IF NOT EXISTS saved_games (
      id TEXT PRIMARY KEY,
      username TEXT NOT NULL,
      name TEXT NOT NULL,
      board TEXT NOT NULL,
      generation INTEGER NOT NULL,
      score INTEGER NOT NULL,
      created_at INTEGER NOT NULL
    )
    "])
   (jdbc/execute! datasource ["
    CREATE INDEX IF NOT EXISTS idx_saved_games_username ON saved_games(username)
    "])
   (log/info "Game persistence database initialized")))

;; ------------------------------------------------------------
;; State Management (π Synthesis)
;; ------------------------------------------------------------

(defonce ^:private games (atom {}))
(defonce ^:private cleanup-executor (atom nil))
(defonce ^:private consecutive-failures (atom 0))
(def ^:private max-consecutive-failures 5)

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
  (and (<= config/board-min-x x config/board-max-x)
       (<= config/board-min-y y config/board-max-y)))

(defn- count-neighbors
  "Count living neighbors for each cell on the board."
  [board]
  (frequencies (mapcat neighbors board)))

(defn- next-generation
  "Evolve board one generation using Conway's rules.
   τ (Wisdom): Uses transient collections for performance.
   ∀ (Vigilance): Bounds checking is applied at creation time and when adding cells."
  [board]
  (let [neighbor-counts (count-neighbors board)]
    (persistent!
     (reduce (fn [acc [cell count]]
               (let [alive? (contains? board cell)]
                 (if (or (= count config/conway-birth-neighbors)
                         (and alive? (<= config/conway-survival-min count config/conway-survival-max)))
                   (conj! acc cell)
                   acc)))
             (transient #{})
             neighbor-counts))))

(defn- calculate-game-score
  "Pure function to calculate score from game state.
   Uses logarithmic scaling to prevent unbounded score growth (μ Directness)."
  [{:keys [board generation]}]
  (let [living-cells (count board)
        ;; Logarithmic complexity score: compresses exponential growth into additive scale
        complexity-score (max config/score-minimum
                              (int (* 100 (Math/log (+ 1 (* living-cells (min generation config/score-generation-cap)))))))
        stability-bonus (if (> generation config/score-stability-threshold)
                          config/score-stability-bonus
                          0)]
    (+ complexity-score stability-bonus)))

;; ------------------------------------------------------------
;; Internal State Updates (τ Wisdom)
;; ------------------------------------------------------------

(defn- update-game!
  "Internal helper for updating game state in: atom.
   Returns: updated game state or nil if game-id not found.
   Thread-safe: uses swap-vals! and returns: new state atomically."
  [game-id update-fn]
  (let [[_ updated-games] (swap-vals! games (fn [m]
                                              (if-let [game (get m game-id)]
                                                (assoc m game-id (update-fn game))
                                                m)))]
    (get-in updated-games [game-id])))

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
     (swap! games (fn [m]
                    (if (>= (count m) config/max-games)
                      (do
                        (log/warnf "Game creation rejected: max-games limit %d reached" config/max-games)
                        (throw (ex-info "Maximum number of games reached"
                                       {:error :max-games-reached
                                        :max-games config/max-games
                                        :current-count (count m)}))
                        m)
                      (assoc m game-id game))))
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
          density (min 1.0 (/ cell-count config/music-max-cells-normalization))
          density-triggers (cond
                             (> cell-count config/music-density-high-threshold)
                             [{:trigger :density-high
                               :params {:frequency config/music-freq-high-density
                                        :amplitude config/music-amp-high-density}}]

                             (> cell-count config/music-density-mid-threshold)
                             [{:trigger :density-mid
                               :params {:frequency config/music-freq-mid-density
                                        :amplitude config/music-amp-mid-density}}]

                             :else [])
          life-trigger (when (pos? cell-count)
                         {:trigger :life-pulse
                          :params {:rate (/ 1.0 (max 1 cell-count))
                                   :intensity density}})
          drone-trigger {:trigger :drone
                         :params {:frequency config/music-freq-drone
                                  :amplitude config/music-amp-drone}}]
      (vec (concat density-triggers
                   (when life-trigger [life-trigger])
                   [drone-trigger])))))

;; ------------------------------------------------------------
;; Game Persistence (∃ Truth - save/load functionality)
;; ------------------------------------------------------------

(defn- generate-id
  "Generate unique ID for saved game."
  []
  (str (java.util.UUID/randomUUID)))

(defn save-game!
  "Save game state to database.
   Returns map with :id, :name, :generation, :score.
   nil if game-id not found."
  [game-id name]
  (when-let [game (get @games game-id)]
    (let [datasource (ds)
          id (generate-id)
          now (System/currentTimeMillis)
          board-json (json/write-str (vec (:board game)))]
      (jdbc/execute-one! datasource
        ["INSERT INTO saved_games (id, username, name, board, generation, score, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
         id name name board-json (:generation game) (calculate-game-score game) now])
      {:id id
       :name name
       :generation (:generation game)
       :score (calculate-game-score game)})))

(defn load-game!
  "Load game from database and create new game state.
   Returns board (set of [x y] coordinates) or nil if not found."
  [saved-id game-id]
  (let [datasource (ds)
        result (jdbc/execute-one! datasource
                 ["SELECT board, generation FROM saved_games WHERE id = ?" saved-id])]
    (when result
      (let [board-coords (json/read-str (:board result) :key-fn keyword)]
        (create-game! game-id (set board-coords))
        (update-game! game-id #(assoc % :generation (:generation result)))
        (get-board game-id)))))

(defn delete-game!
  "Delete saved game from database.
   Returns nil (no-op if not found)."
  [saved-id]
  (let [datasource (ds)]
    (jdbc/execute-one! datasource
      ["DELETE FROM saved_games WHERE id = ?" saved-id])
    nil))

(defn list-saved-games
  "List all saved games.
   Returns vector of maps with :id, :name, :generation, :score."
  []
  (let [datasource (ds)]
    (jdbc/execute! datasource
      ["SELECT id, name, generation, score, created_at FROM saved_games ORDER BY created_at DESC"]
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ------------------------------------------------------------
;; Lifecycle (τ Wisdom)
;; ------------------------------------------------------------

(defn cleanup-stale-games!
  []
  (let [result (atom nil)
        [updated-games _] (swap-vals! games (fn [m]
                                              (let [now (System/currentTimeMillis)
                                                    cutoff (- now config/game-ttl-ms)
                                                    before (count m)
                                                    filtered (into {} (filter (fn [[_ g]] (> (:last-accessed g) cutoff)) m))
                                                    removed (- before (count filtered))]
                                                (reset! result removed)
                                                filtered)))
        removed @result]
    (when (pos? removed)
      (log/infof "Game cleanup: removed %d stale games" removed))
    removed))

(defn stop-cleanup-scheduler!
  []
  (when-let [executor @cleanup-executor]
    (.shutdown executor)
    (reset! cleanup-executor nil)))

(defn start-cleanup-scheduler!
  []
  (when-not @cleanup-executor
    (let [thread-factory (reify java.util.concurrent.ThreadFactory
                           (newThread [_ runnable]
                             (doto (Thread. runnable)
                               (.setDaemon true)
                               (.setName "game-cleanup-scheduler"))))
          executor (java.util.concurrent.Executors/newSingleThreadScheduledExecutor thread-factory)
          task (fn []
                 (try
                   (cleanup-stale-games!)
                   (reset! consecutive-failures 0)
                   (catch Exception e
                     (log/error e "Cleanup failed")
                     (swap! consecutive-failures inc)
                     (when (>= @consecutive-failures max-consecutive-failures)
                       (log/errorf "Stopping cleanup scheduler after %d consecutive failures" max-consecutive-failures)
                       (stop-cleanup-scheduler!)))))]
      (.scheduleAtFixedRate executor
                            task
                            config/cleanup-initial-delay-minutes
                            config/cleanup-interval-minutes
                            java.util.concurrent.TimeUnit/MINUTES)
      (reset! cleanup-executor executor))))

(defn initialize!
  []
  (reset! games {})
  (start-cleanup-scheduler!))

(defn health-check
  "Check game engine health status.
   Returns health information for monitoring."
  []
  (let [scheduler-running? (some? @cleanup-executor)
        active-games (count @games)]
    {:healthy? scheduler-running?
     :details {:scheduler-running scheduler-running?
               :active-games active-games
               :timestamp (System/currentTimeMillis)}}))
