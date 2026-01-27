(ns cc.mindward.test-assertion-verification
  "Test assertion count verification to ensure documentation accuracy.
   
   ∃ (Truth): Verify actual test counts match documented claims.
   ∀ (Vigilance): Prevent documentation drift and maintain truth.
   π (Synthesis): Holistic verification of test infrastructure."
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn count-test-assertions
  "Count total assertions across all test suites.
   
   Returns total assertion count by running tests with custom reporter."
  []
  (let [assertion-count (atom 0)
        error-count (atom 0)
        test-count (atom 0)]

    ;; Custom reporter to count assertions
    (defmethod t/report :pass [m]
      (swap! assertion-count inc))

    (defmethod t/report :fail [m]
      (swap! assertion-count inc))

    (defmethod t/report :error [m]
      (swap! assertion-count inc)
      (swap! error-count inc))

    (defmethod t/report :begin-test-ns [m]
      (swap! test-count inc))

    ;; Run all tests
    (let [results (t/run-all-tests #".*-test$")]
      {:assertions @assertion-count
       :tests (:test results)
       :failures (:fail results)
       :errors (:error results)})))

(defn read-documented-count
  "Extract assertion count from documentation files.
   
   Searches README.md and PRACTICAL_GUIDE.md for assertion counts."
  []
  (let [readme (slurp (io/file "README.md"))
        practical-guide (slurp (io/file "PRACTICAL_GUIDE.md"))

        ;; Pattern: "658 assertions" or "658 passing assertions"
        pattern #"(\d+)\s+(?:passing\s+)?assertions"

        readme-match (re-find pattern readme)
        guide-match (re-find pattern practical-guide)]

    {:readme (when readme-match (Integer/parseInt (second readme-match)))
     :practical-guide (when guide-match (Integer/parseInt (second guide-match)))}))

(defn verify-assertion-consistency
  "Verify that test counts match across documentation and actual tests.
   
   Returns map with verification results and any discrepancies."
  []
  (let [actual-counts (count-test-assertions)
        documented-counts (read-documented-count)
        actual-assertions (:assertions actual-counts)]

    {:actual actual-assertions
     :documented documented-counts
     :readme-match? (= actual-assertions (:readme documented-counts))
     :guide-match? (= actual-assertions (:practical-guide documented-counts))
     :all-tests-pass? (and (zero? (:failures actual-counts))
                           (zero? (:errors actual-counts)))}))

;; Test suite for assertion verification
(deftest ^:metadata test-assertion-count-verification
  (let [verification (verify-assertion-consistency)
        actual (:actual verification)
        documented (:documented verification)]

    (t/testing "Test assertion count matches documentation (∃ Truth)"
      (t/testing "README.md assertion count"
        (if (:readme documented)
          (t/is (= actual (:readme documented))
                (format "README.md claims %d assertions, but tests have %d"
                        (:readme documented) actual))
          (t/is false "README.md doesn't specify assertion count")))

      (t/testing "PRACTICAL_GUIDE.md assertion count"
        (if (:practical-guide documented)
          (t/is (= actual (:practical-guide documented))
                (format "PRACTICAL_GUIDE.md claims %d assertions, but tests have %d"
                        (:practical-guide documented) actual))
          (t/is false "PRACTICAL_GUIDE.md doesn't specify assertion count"))))

    (t/testing "All tests pass (∀ Vigilance)"
      (t/is (:all-tests-pass? verification)
            "Some tests are failing"))

    ;; Output informative message
    (println "\n📊 Test Assertion Verification")
    (println "=" 50)
    (println "Actual assertions:" actual)
    (println "README.md claims:" (:readme documented))
    (println "PRACTICAL_GUIDE.md claims:" (:practical-guide documented))
    (println "All tests pass?:" (:all-tests-pass? verification))
    (println "=" 50)))

(deftest ^:metadata test-documentation-consistency
  "Verify documentation consistency across project (π Synthesis)."
  (let [readme (slurp (io/file "README.md"))
        practical-guide (slurp (io/file "PRACTICAL_GUIDE.md"))
        simplicity (slurp (io/file "SIMPLICITY.md"))
        agents (slurp (io/file "AGENTS.md"))]

    (t/testing "Core documents reference each other"
      (t/is (str/includes? readme "SIMPLICITY.md")
            "README.md should reference SIMPLICITY.md")
      (t/is (str/includes? readme "PRACTICAL_GUIDE.md")
            "README.md should reference PRACTICAL_GUIDE.md")
      (t/is (str/includes? readme "AGENTS.md")
            "README.md should reference AGENTS.md"))

    (t/testing "Mathematical principles are documented"
      (t/is (str/includes? simplicity "Euler's Formula")
            "SIMPLICITY.md should mention Euler's Formula")
      (t/is (str/includes? simplicity "Pythagorean Theorem")
            "SIMPLICITY.md should mention Pythagorean Theorem")
      (t/is (str/includes? simplicity "Logarithms")
            "SIMPLICITY.md should mention Logarithms"))

    (t/testing "Sarcasmotron methodology is documented"
      (t/is (str/includes? simplicity "sarcasmotron")
            "SIMPLICITY.md should document sarcasmotron methodology"))))

(deftest ^:metadata test-mathematical-patterns-exist
  "Verify mathematical pattern implementations exist (e Purpose)."
  (let [math-patterns-file (io/file "components/game/src/cc/mindward/game/mathematical_patterns.clj")]

    (t/testing "Mathematical patterns file exists"
      (t/is (.exists math-patterns-file)
            "Mathematical patterns file should exist"))

    (when (.exists math-patterns-file)
      (let [content (slurp math-patterns-file)]
        (t/testing "Contains key mathematical principles"
          (t/is (str/includes? content "PYTHAGOREAN THEOREM")
                "Should implement Pythagorean theorem patterns")
          (t/is (str/includes? content "LOGARITHMS")
                "Should implement logarithmic patterns")
          (t/is (str/includes? content "CALCULUS")
                "Should implement calculus patterns")
          (t/is (str/includes? content "CHAOS THEORY")
                "Should implement chaos theory patterns"))))))

(defn run-verification []
  "Run all verification tests and return summary."
  (let [results (t/run-tests 'cc.mindward.test-assertion-verification)]
    {:assertion-verification (-> results :pass (get 0))
     :documentation-consistency (-> results :pass (get 1))
     :mathematical-patterns (-> results :pass (get 2))
     :total-tests (:test results)
     :total-assertions (:pass results)
     :failures (:fail results)
     :errors (:error results)}))

(comment
  ;; Run verification manually
  (run-verification)

  ;; Check assertion counts
  (count-test-assertions)

  ;; Read documented counts
  (read-documented-count)

  ;; Full verification
  (verify-assertion-consistency)

  ;; Update documentation if counts differ
  (let [actual (-> (count-test-assertions) :assertions)
        readme (slurp (io/file "README.md"))
        updated (str/replace readme #"\d+(?=\s+passing assertions)" (str actual))]
    (spit "README.md" updated)
    (println "Updated README.md with" actual "assertions")))