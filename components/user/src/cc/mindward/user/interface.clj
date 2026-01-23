(ns cc.mindward.user.interface
  "User component public interface.
   
   All user-related operations flow through this namespace.
   Implementation details (storage, hashing) are encapsulated."
  (:require [cc.mindward.user.impl :as impl]
            [cc.mindward.user.validation :as validation]
            [clojure.spec.alpha :as s]))

;; ------------------------------------------------------------
;; Domain Specs (∃ Truth)
;; ------------------------------------------------------------

(s/def :user/username
       (s/and string?
              #(>= (count %) validation/username-min-length)
              #(<= (count %) validation/username-max-length)
              #(re-matches validation/username-pattern %)))

(s/def :user/password
       (s/and string? #(>= (count %) validation/password-min-length)))

(s/def :user/name
       (s/and string?
              #(>= (count %) validation/name-min-length)
              #(<= (count %) validation/name-max-length)))

(s/def :user/score
       (s/and int? #(>= % 0) #(<= % validation/score-max-value)))

(s/def :user/user-map
       (s/keys :req-un [:user/username :user/password :user/name]))

(s/def :user/leaderboard-entry
       (s/keys :req-un [:user/username :user/name]
               :req [:user/high_score]))

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

(defn health-check
  "Check user database health status.
   Returns {:healthy? true/false :details {...}}"
  []
  (impl/health-check))

;; ------------------------------------------------------------
;; Input Validation (∃ Truth - single source of truth)
;; ------------------------------------------------------------

;; Re-export validation functions
(def validate-username validation/validate-username)
(def validate-password validation/validate-password)
(def validate-name validation/validate-name)
(def validate-score validation/validate-score)
(def valid-username? validation/valid-username?)
(def valid-password? validation/valid-password?)
(def valid-name? validation/valid-name?)

;; Re-export validation constants
(def username-min-length validation/username-min-length)
(def username-max-length validation/username-max-length)
(def username-pattern validation/username-pattern)
(def password-min-length validation/password-min-length)
(def name-min-length validation/name-min-length)
(def name-max-length validation/name-max-length)
(def score-max-value validation/score-max-value)
