(ns user
  (:require [clojure.tools.namespace.repl :as tools-ns]))

(defn reset []
  (tools-ns/refresh))

(comment
  (reset)
  )
