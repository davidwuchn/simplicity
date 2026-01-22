(ns user
  (:require [nextjournal.clerk :as clerk]))

(comment
  ;; Clerk is configured but notebooks not yet created
  ;; To use Clerk:
  ;; 1. Create notebooks/ directory
  ;; 2. Add .clj or .md notebook files
  ;; 3. Start the server:
  (clerk/serve! {:browse? true})

  ;; With file watching:
  (clerk/serve! {:watch-paths ["notebooks"]})
  
  ;; Example notebooks (create these first):
  ;; (clerk/show! "notebooks/game_of_life_demo.clj")
  ;; (clerk/show! "notebooks/musical_patterns.clj")
  
  (clerk/clear-cache!)
  )
