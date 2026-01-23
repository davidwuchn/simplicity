(ns cc.mindward.ui.interface
  "Public interface for UI component.
   
   This namespace delegates to individual page namespaces,
   maintaining backward compatibility with existing function signatures."
  (:require [cc.mindward.ui.pages.leaderboard :as leaderboard]
            [cc.mindward.ui.pages.auth :as auth]
            [cc.mindward.ui.pages.game :as game]
            [cc.mindward.ui.pages.landing :as landing]))

(defn leaderboard-page
  "Render the leaderboard page."
  [session leaderboard]
  (leaderboard/leaderboard-page {:session session :leaderboard leaderboard}))

(defn signup-page
  "Render the signup page."
  [session params anti-forgery-token]
  (auth/signup-page {:session session :params params :anti-forgery-token anti-forgery-token}))

(defn game-page
  "Render the game page."
  [session anti-forgery-token high-score]
  (game/game-page session anti-forgery-token high-score))

(defn landing-page
  "Render the landing page."
  [session]
  (landing/landing-page {:session session}))

(defn login-page
  "Render the login page."
  [session params anti-forgery-token]
  (auth/login-page {:session session :params params :anti-forgery-token anti-forgery-token}))
