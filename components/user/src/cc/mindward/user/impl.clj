(ns cc.mindward.user.impl
  "User persistence implementation.
   
   Design Pattern: Repository Pattern with injectable datasource.
   The datasource is initialized lazily via `ensure-db!` and can be
   rebound for testing via `with-redefs` on the `*ds*` dynamic var."
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [buddy.hashers :as hashers]))

;; ------------------------------------------------------------
;; Database Configuration (τ Wisdom - lazy initialization)
;; ------------------------------------------------------------

(defn make-datasource
  "Create a datasource from configuration.
   Extracted for testability - tests can call this with temp files."
  [db-path]
  (jdbc/get-datasource {:dbtype "sqlite" :dbname db-path}))

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
;; Algorithm: bcrypt+sha512 (buddy-hashers default)
;; ------------------------------------------------------------

(defn hash-password
  "Hash a plain-text password. Returns the hash string."
  [password]
  (hashers/derive password))

(defn verify-password
  "Verify a plain-text password against a stored hash.
   Returns true if the password matches, false otherwise."
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

(defn health-check
  "Check user database health status.
   Returns health information for monitoring."
  []
  (try
    (let [datasource @db-state
          result (jdbc/execute! (ds) ["SELECT 1"])
          user-count (-> (jdbc/execute! (ds) ["SELECT COUNT(*) as count FROM users"]
                                       {:builder-fn rs/as-unqualified-lower-maps})
                        first
                        :count)]
      {:healthy? true
       :details {:database-connected true
                 :datasource-initialized (some? datasource)
                 :user-count user-count
                 :timestamp (System/currentTimeMillis)}})
    (catch Exception e
      {:healthy? false
       :details {:database-connected false
                 :error (.getMessage e)
                 :timestamp (System/currentTimeMillis)}})))
