#!/usr/bin/env clojure
(require '[clojure.test :as test]
         '[cc.mindward.user.interface-test]
         '[cc.mindward.auth.interface-test]
         '[cc.mindward.game.interface-test])

(let [results (test/run-all-tests #".*-test$")]
  (println "\n=== All Tests Summary ===")
  (println "Tests:" (:test results))
  (println "Assertions:" (:pass results))
  (println "Failures:" (:fail results))
  (println "Errors:" (:error results))
  (System/exit (if (or (pos? (:fail results 0))
                       (pos? (:error results 0))) 1 0)))
