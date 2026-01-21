(ns cc.mindward.auth.impl
  (:require [cc.mindward.user.interface :as user]))

(defn hello [name]
  (str "Hello, " name ". Access granted to Mindward."))

(defn authenticate [username password]
  (let [u (user/find-by-username username)]
    (and u (= (:password u) password) u)))
