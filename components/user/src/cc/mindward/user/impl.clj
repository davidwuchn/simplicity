(ns cc.mindward.user.impl
  "User persistence implementation.
   
   Design Pattern: Repository Pattern with injectable datasource.
   The datasource is initialized lazily via `ensure-db!` and can be
   rebound for testing via `with-redefs` on the `*ds*` dynamic var.
   
   Performance (τ Wisdom): Uses HikariCP connection pooling for production."
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [buddy.hashers :as hashers]
            [clojure.data.json :as json])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]))

;; ------------------------------------------------------------
;; Database Configuration (τ Wisdom - connection pooling)
;; ------------------------------------------------------------

(defn make-datasource
  "Create a pooled datasource using HikariCP.
   
   Configuration:
   - Maximum pool size: 10 connections
   - Minimum idle connections: 2
   - Connection timeout: 30 seconds
   - Idle timeout: 10 minutes
   - Max lifetime: 30 minutes
   
   For testing, pass :pool? false to use simple datasource.
   
   (τ Wisdom): Connection pooling reduces overhead and improves throughput."
  ([db-path] (make-datasource db-path {:pool? true}))
  ([db-path {:keys [pool?] :or {pool? true}}]
   (if pool?
     ;; Production: Use HikariCP connection pool
     (let [config (doto (HikariConfig.)
                    (.setJdbcUrl (str "jdbc:sqlite:" db-path))
                    (.setMaximumPoolSize 10)
                    (.setMinimumIdle 2)
                    (.setConnectionTimeout 30000)  ; 30 seconds
                    (.setIdleTimeout 600000)       ; 10 minutes
                    (.setMaxLifetime 1800000)      ; 30 minutes
                    (.setPoolName "simplicity-pool"))]
       (HikariDataSource. config))
     ;; Testing: Simple datasource (no pooling overhead)
     (jdbc/get-datasource {:dbtype "sqlite" :dbname db-path}))))

(defonce ^:private db-state (atom nil))

(defn get-datasource
  "Get or create the datasource. Lazy initialization avoids side effects on require."
  []
  (or @db-state
      (let [db-path (or (System/getenv "DB_PATH") "simplicity.db")
            ds (make-datasource db-path)]
        (reset! db-state ds)
        ds)))

;; Dynamic var for test rebinding
(def ^:dynamic *ds* nil)

(defn ds
  "Get the current datasource. Uses *ds* if bound (for tests), otherwise get-datasource."
  []
  (or *ds* (get-datasource)))

(defn init-db!
  "Initialize the database schema. Idempotent - safe to call multiple times.
   Must be called explicitly before first use."
  ([] (init-db! (ds)))
  ([datasource]
   (jdbc/execute! datasource ["
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      name TEXT NOT NULL,
      high_score INTEGER DEFAULT 0
    )"])
   (jdbc/execute! datasource ["
    CREATE TABLE IF NOT EXISTS saved_games (
      id TEXT PRIMARY KEY,
      username TEXT NOT NULL,
      name TEXT NOT NULL,
      board_json TEXT NOT NULL,
      generation INTEGER NOT NULL,
      score INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      saved_at INTEGER NOT NULL,
      FOREIGN KEY(username) REFERENCES users(username)
    )"])
   ;; Seed admin user if configured (∀ Vigilance - no hardcoded secrets)
   (let [admin-user (System/getenv "ADMIN_USER")
         admin-pass (System/getenv "ADMIN_PASS")]
     (when (and admin-user admin-pass)
       (let [existing (jdbc/execute-one! datasource 
                        ["SELECT * FROM users WHERE username = ?" admin-user])]
         (when-not existing
           (jdbc/execute! datasource 
             ["INSERT INTO users (username, password_hash, name) VALUES (?, ?, ?)"
              admin-user (hashers/derive admin-pass) "System Admin"])))))))

;; ------------------------------------------------------------
;; Password Hashing (∃ Truth - security is non-negotiable)
;; Algorithm: bcrypt+sha512 with increased work factor
;; ------------------------------------------------------------

(def ^:private bcrypt-work-factor
  "Bcrypt work factor (cost parameter).
   
   Security (τ Wisdom): Cost 12 provides ~4x stronger hashing than default cost 10.
   Recommended minimum: 12 (as of 2024). Higher is stronger but slower.
   
   Computation time at cost 12: ~200-400ms per hash (acceptable for login)."
  12)

(defn hash-password
  "Hash a plain-text password with bcrypt+sha512.
   
   Security (∀ Vigilance): Uses elevated work factor to resist GPU cracking attacks.
   Returns the hash string with embedded algorithm and cost parameters."
  [password]
  (hashers/derive password {:alg :bcrypt+sha512
                            :work-factor bcrypt-work-factor}))

(defn verify-password
  "Verify a plain-text password against a stored hash.
   Returns true if the password matches, false otherwise.
   
   Security: buddy-hashers/check automatically handles variable work factors
   and will correctly verify hashes regardless of their embedded cost."
  [password hash]
  (hashers/check password hash))

;; ------------------------------------------------------------
;; Repository Functions (λ - pure data in, data out at boundary)
;; ------------------------------------------------------------

(defn find-by-username [username]
  (jdbc/execute-one! (ds) ["SELECT * FROM users WHERE username = ?" username]
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn update-high-score! [username score]
  (jdbc/execute! (ds) ["UPDATE users SET high_score = MAX(high_score, ?) WHERE username = ?" score username]))

(defn get-high-score [username]
  (:high_score (find-by-username username)))

(defn get-leaderboard []
  (jdbc/execute! (ds) ["SELECT username, name, high_score FROM users ORDER BY high_score DESC LIMIT 10"]
                 {:builder-fn rs/as-unqualified-lower-maps}))

(defn create-user!
  "Create a new user. Password is hashed before storage.
   Throws on duplicate username (SQLite UNIQUE constraint)."
  [{:keys [username password name]}]
  (jdbc/execute! (ds) ["INSERT INTO users (username, password_hash, name) VALUES (?, ?, ?)"
                       username (hash-password password) name]))

;; ------------------------------------------------------------
;; Game Persistence (Fixed Memory Leak)
;; ------------------------------------------------------------

(defn save-game!
  "Save a game state to the database."
  [username name board generation score]
  (let [id (str (java.util.UUID/randomUUID))
        board-json (json/write-str board)
        now (System/currentTimeMillis)]
    (jdbc/execute! (ds)
                   ["INSERT INTO saved_games (id, username, name, board_json, generation, score, created_at, saved_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    id username name board-json generation score now now])
    {:id id :name name :saved true}))

(defn load-game!
  "Load a game state by ID. Returns map with :board (vector of vectors), :generation."
  [id]
  (let [row (jdbc/execute-one! (ds)
                               ["SELECT board_json, generation FROM saved_games WHERE id = ?" id]
                               {:builder-fn rs/as-unqualified-lower-maps})]
    (when row
      {:board (json/read-str (:board_json row))
       :generation (:generation row)
       :loaded true})))

(defn list-saved-games
  "List all saved games. Optionally filter by username."
  ([] (list-saved-games nil))
  ([username]
   (let [query (if username
                 ["SELECT id, name, generation, score FROM saved_games WHERE username = ? ORDER BY saved_at DESC" username]
                 ["SELECT id, name, generation, score FROM saved_games ORDER BY saved_at DESC"])]
     (jdbc/execute! (ds) query
                    {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-game!
  "Delete a saved game by ID."
  [id]
  (jdbc/execute! (ds) ["DELETE FROM saved_games WHERE id = ?" id]))

(defn health-check
  "Check user database health status.
   Returns health information for monitoring.
   
   Includes connection pool statistics when using HikariCP."
  []
  (try
    (let [datasource @db-state
          _result (jdbc/execute! (ds) ["SELECT 1"])  ;; Health check query
          user-count (-> (jdbc/execute! (ds) ["SELECT COUNT(*) as count FROM users"]
                                       {:builder-fn rs/as-unqualified-lower-maps})
                        first
                        :count)
          ;; Get HikariCP pool stats if available
          pool-stats (when (instance? HikariDataSource datasource)
                       (let [pool-mxbean (.getHikariPoolMXBean ^HikariDataSource datasource)]
                         {:active-connections (.getActiveConnections pool-mxbean)
                          :idle-connections (.getIdleConnections pool-mxbean)
                          :total-connections (.getTotalConnections pool-mxbean)
                          :threads-awaiting-connection (.getThreadsAwaitingConnection pool-mxbean)}))]
      {:healthy? true
       :details (merge {:database-connected true
                        :datasource-initialized (some? datasource)
                        :user-count user-count
                        :timestamp (System/currentTimeMillis)}
                       (when pool-stats
                         {:connection-pool pool-stats}))})
    (catch Exception e
      {:healthy? false
       :details {:database-connected false
                 :error (.getMessage e)
                 :timestamp (System/currentTimeMillis)}})))
