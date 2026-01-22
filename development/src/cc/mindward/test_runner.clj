(ns cc.mindward.test-runner
  (:require [clojure.test :as test]
            [cc.mindward.user.interface-test]
            [cc.mindward.auth.interface-test]))

(defn -main [& _]
  (println "Running Simplicity tests...")
  (let [results (test/run-all-tests #".*-test$")]
    (println "\n=== Test Summary ===")
    (println "Tests:" (:test results))
    (println "Assertions:" (:pass results))
    (println "Failures:" (:fail results))
    (println "Errors:" (:error results))
    (System/exit (if (or (pos? (:fail results)) (pos? (:error results))) 1 0))))
