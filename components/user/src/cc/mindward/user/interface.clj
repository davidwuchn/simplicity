(ns cc.mindward.user.interface
  (:require [cc.mindward.user.impl :as impl]))

(defn find-by-username [username]
  (impl/find-by-username username))

(defn update-high-score! [username score]
  (impl/update-high-score! username score))

(defn get-high-score [username]
  (impl/get-high-score username))

(defn get-leaderboard []
  (impl/get-leaderboard))

(defn create-user! [user-map]
  (impl/create-user! user-map))
