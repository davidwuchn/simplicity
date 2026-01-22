(ns cc.mindward.ui.interface
  (:require [cc.mindward.ui.core :as core]))

(defn leaderboard-page [session leaderboard]
  (core/leaderboard-page session leaderboard))

(defn signup-page [session params anti-forgery-token]
  (core/signup-page session params anti-forgery-token))

(defn game-page [session anti-forgery-token high-score]
  (core/game-page session anti-forgery-token high-score))

(defn landing-page [session]
  (core/landing-page session))

(defn login-page [session params anti-forgery-token]
  (core/login-page session params anti-forgery-token))
