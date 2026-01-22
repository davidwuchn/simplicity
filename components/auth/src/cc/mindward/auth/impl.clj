(ns cc.mindward.auth.impl
  "Authentication implementation.
   
   Design Pattern: Authentication uses the user component for
   credential storage and password verification."
  (:require [cc.mindward.user.interface :as user]))

(defn hello [name]
  (str "Hello, " name ". Access granted to Mindward."))

(defn authenticate
  "Authenticate a user by username and password.
   Returns the user map (without password_hash) on success, nil on failure.
   
   Security (∃ Truth): Passwords are verified against bcrypt hashes,
   never compared as plain text."
  [username password]
  (when-let [u (user/find-by-username username)]
    (when (user/verify-password password (:password_hash u))
      (dissoc u :password_hash))))
