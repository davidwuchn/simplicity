(ns cc.mindward.auth.interface
  (:require [cc.mindward.auth.impl :as impl]))

(defn hello [name]
  (impl/hello name))

(defn authenticate [username password]
  (impl/authenticate username password))
