(ns cc.mindward.auth.interface
  (:require [cc.mindward.auth.impl :as impl]))

(defn authenticate [username password]
  (impl/authenticate username password))
