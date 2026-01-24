(ns cc.mindward.test-runner
  (:require [clojure.test :as test]
            [cc.mindward.user.interface-test]
            [cc.mindward.user.security-test]
            [cc.mindward.auth.interface-test]
            [cc.mindward.auth.documentation-test]
            [cc.mindward.game.interface-test]
            #_[cc.mindward.game.property-test]
            #_[cc.mindward.game.performance-test]
            [cc.mindward.ui.core-test]
            [cc.mindward.ui.components-test]
            [cc.mindward.ui.helpers-test]
            [cc.mindward.web-server.core-test]
            [cc.mindward.web-server.security-test]))

(defn -main [& _]
  (println "Running Simplicity tests...")
  (let [results (test/run-all-tests #".*-test$")]
    (println "\n=== Test Summary ===")
    (println "Tests:" (:test results))
    (println "Assertions:" (:pass results))
    (println "Failures:" (:fail results))
    (println "Errors:" (:error results))
    (System/exit (if (or (pos? (:fail results)) (pos? (:error results))) 1 0))))
