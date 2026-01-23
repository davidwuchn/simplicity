(ns cc.mindward.auth.interface
  "Authentication component public interface.
   
   Handles user authentication with secure password verification.
   Delegates to impl for bcrypt hashing and comparison logic."
  (:require [cc.mindward.auth.impl :as impl]))

(defn authenticate
  "Authenticate a user with username and password.
   
   Args:
     username - String username to authenticate
     password - Plain-text password to verify
   
   Returns:
     User map if credentials are valid, nil otherwise.
   
   Security: Uses bcrypt + sha512 with timing attack resistance."
  [username password]
  (impl/authenticate username password))
