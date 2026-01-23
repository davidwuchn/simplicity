(ns cc.mindward.ui.interface
  "Public interface for UI component.

   This namespace delegates to individual page namespaces,
   maintaining backward compatibility with existing function signatures."
  (:require [cc.mindward.ui.pages.leaderboard :as leaderboard]
            [cc.mindward.ui.pages.auth :as auth]
            [cc.mindward.ui.pages.shooter.core :as shooter]
            [cc.mindward.ui.pages.game-life :as game-life]
            [cc.mindward.ui.pages.select-game :as select-game]
            [cc.mindward.ui.pages.landing :as landing]))

(defn leaderboard-page
  "Render the leaderboard page."
  [session leaderboard]
  (leaderboard/leaderboard-page {:session session :leaderboard leaderboard}))

(defn signup-page
  "Render the signup page."
  [session params anti-forgery-token]
  (auth/signup-page {:session session :params params :anti-forgery-token anti-forgery-token}))

(defn shooter-page
  "Render the Space Shooter game page."
  [session anti-forgery-token high-score]
  (shooter/shooter-page session anti-forgery-token high-score))

(defn game-life-page
  "Render Conway's Game of Life page."
  [session]
  (game-life/game-life-page {:session session}))

(defn select-game-page
  "Render the game selection page."
  [session]
  (select-game/select-game-page {:session session}))

(defn landing-page
  "Render the landing page."
  [session]
  (landing/landing-page {:session session}))

(defn login-page
  "Render the login page."
  [session params anti-forgery-token]
  (auth/login-page {:session session :params params :anti-forgery-token anti-forgery-token}))
