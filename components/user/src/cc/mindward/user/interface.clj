(ns cc.mindward.user.interface
  "User component public interface.
   
   All user-related operations flow through this namespace.
   Implementation details (storage, hashing) are encapsulated."
  (:require [cc.mindward.user.impl :as impl]))

;; ------------------------------------------------------------
;; Lifecycle
;; ------------------------------------------------------------

(defn init!
  "Initialize the user subsystem. Call once at application startup.
   Idempotent - safe to call multiple times."
  []
  (impl/init-db!))

;; ------------------------------------------------------------
;; Query Operations (λ - pure lookups)
;; ------------------------------------------------------------

(defn find-by-username
  "Find a user by username. Returns user map or nil."
  [username]
  (impl/find-by-username username))

(defn get-high-score
  "Get the high score for a username. Returns integer or nil."
  [username]
  (impl/get-high-score username))

(defn get-leaderboard
  "Get the top 10 users by high score.
   Returns vector of maps with :username, :name, :high_score."
  []
  (impl/get-leaderboard))

;; ------------------------------------------------------------
;; Command Operations (! suffix - side effects)
;; ------------------------------------------------------------

(defn update-high-score!
  "Update user's high score if the new score is higher.
   Uses SQL MAX() for atomic comparison."
  [username score]
  (impl/update-high-score! username score))

(defn create-user!
  "Create a new user. Expects map with :username, :password, :name.
   Password is hashed before storage. Throws on duplicate username."
  [user-map]
  (impl/create-user! user-map))

;; ------------------------------------------------------------
;; Authentication Support
;; ------------------------------------------------------------

(defn verify-password
  "Verify a plain-text password against a user's stored hash.
   Returns true if valid, false otherwise."
  [password password-hash]
  (impl/verify-password password password-hash))
