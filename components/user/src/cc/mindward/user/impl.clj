(ns cc.mindward.user.impl
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def db-path (or (System/getenv "DB_PATH") "simplicity.db"))
(def db-spec {:dbtype "sqlite" :dbname db-path})

(def ds (jdbc/get-datasource db-spec))

(defn init-db! []
  (jdbc/execute! ds ["
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      name TEXT NOT NULL,
      high_score INTEGER DEFAULT 0
    )"])
  ;; Seed admin user if configured
  (let [admin-user (System/getenv "ADMIN_USER")
        admin-pass (System/getenv "ADMIN_PASS")]
    (when (and admin-user admin-pass)
      (let [existing (jdbc/execute-one! ds ["SELECT * FROM users WHERE username = ?" admin-user])]
        (when-not existing
          (jdbc/execute! ds ["INSERT INTO users (username, password, name) VALUES (?, ?, ?)"
                             admin-user admin-pass "System Admin"]))))))

;; Initialize on load (still not ideal, but better than hardcoded secrets)
(init-db!)

(defn find-by-username [username]
  (jdbc/execute-one! ds ["SELECT * FROM users WHERE username = ?" username]
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn update-high-score! [username score]
  (jdbc/execute! ds ["UPDATE users SET high_score = MAX(high_score, ?) WHERE username = ?" score username]))

(defn get-high-score [username]
  (:high_score (find-by-username username)))

(defn get-leaderboard []
  (jdbc/execute! ds ["SELECT username, name, high_score FROM users ORDER BY high_score DESC LIMIT 10"]
                 {:builder-fn rs/as-unqualified-lower-maps}))

(defn create-user! [{:keys [username password name]}]
  (jdbc/execute! ds ["INSERT INTO users (username, password, name) VALUES (?, ?, ?)"
                     username password name]))
