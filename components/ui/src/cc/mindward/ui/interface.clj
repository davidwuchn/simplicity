(ns cc.mindward.ui.interface
  "Public interface for UI component.
   
   This namespace delegates to the refactored core implementation,
   maintaining backward compatibility with existing function signatures."
  (:require [cc.mindward.ui.core-refactored :as core]))

(defn leaderboard-page
  "Render the leaderboard page."
  [session leaderboard]
  (core/leaderboard-page {:session session :leaderboard leaderboard}))

(defn signup-page
  "Render the signup page."
  [session params anti-forgery-token]
  (core/signup-page {:session session :params params :anti-forgery-token anti-forgery-token}))

(defn game-page
  "Render the game page."
  [session anti-forgery-token high-score]
  (core/game-page session anti-forgery-token high-score))

(defn landing-page
  "Render the landing page."
  [session]
  (core/landing-page {:session session}))

(defn login-page
  "Render the login page."
  [session params anti-forgery-token]
  (core/login-page {:session session :params params :anti-forgery-token anti-forgery-token}))
