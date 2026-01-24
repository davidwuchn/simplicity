(ns cc.mindward.auth.documentation-test
  "Documentation tests for auth component.
   
   Tests that verify function behavior matches documented contracts.
   
   π (Synthesis): Ensures code and documentation form a complete mental model
   ∃ (Truth): Verifies that documentation accurately describes reality"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [cc.mindward.auth.interface :as auth]
            [cc.mindward.user.interface :as user]
            [cc.mindward.user.impl :as user-impl]))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(defn test-db-fixture
  "Fixture that creates a temporary SQLite database for each test."
  [f]
  (let [temp-file (java.io.File/createTempFile "test_doc_" ".db")
        db-path (.getAbsolutePath temp-file)
        test-ds (user-impl/make-datasource db-path {:pool? false})]
    (try
      (binding [user-impl/*ds* test-ds]
        (user-impl/init-db! test-ds)
        (f))
      (finally
        (.delete temp-file)))))

(use-fixtures :each test-db-fixture)

;; ============================================================================
;; Documentation Contract Tests
;; ============================================================================

(deftest authenticate-documentation-contract-test
  (testing "authenticate function matches documented behavior"
    ;; Setup: Create test user
    (user/create-user! {:username "doc-user" :password "doc-pass" :name "Documentation User"})
    
    ;; Documented behavior 1: Returns user map without password_hash for valid credentials
    (testing "returns user map without password_hash for valid credentials"
      (let [result (auth/authenticate "doc-user" "doc-pass")]
        (is (map? result) "Should return a map")
        (is (= "doc-user" (:username result)) "Should include username")
        (is (= "Documentation User" (:name result)) "Should include name")
        (is (nil? (:password_hash result)) "Should NOT include password_hash")
        (is (not (contains? result :password_hash)) "password_hash key should not exist")))
    
    ;; Documented behavior 2: Returns nil for invalid password
    (testing "returns nil for invalid password"
      (is (nil? (auth/authenticate "doc-user" "wrong-password"))
          "Should return nil for wrong password"))
    
    ;; Documented behavior 3: Returns nil for unknown user
    (testing "returns nil for unknown user"
      (is (nil? (auth/authenticate "non-existent-user" "any-password"))
          "Should return nil for non-existent user"))
    
    ;; Documented behavior 4: Handles nil/empty inputs gracefully
    (testing "handles nil/empty inputs gracefully"
      (is (nil? (auth/authenticate nil "password")) "nil username → nil")
      (is (nil? (auth/authenticate "" "password")) "empty username → nil")
      (is (nil? (auth/authenticate "doc-user" nil)) "nil password → nil")
      (is (nil? (auth/authenticate "doc-user" "")) "empty password → nil"))
    
    ;; Documented behavior 5: Case-sensitive username matching
    (testing "case-sensitive username matching"
      (is (nil? (auth/authenticate "DOC-USER" "doc-pass"))
          "Should be case-sensitive (uppercase should fail)")
      (is (nil? (auth/authenticate "Doc-User" "doc-pass"))
          "Should be case-sensitive (mixed case should fail)"))
    
    ;; Documented behavior 6: SQL injection prevention
    (testing "SQL injection prevention"
      (let [injection-attempts ["admin'--" "' OR '1'='1" "'; DROP TABLE users; --"]]
        (doseq [malicious-username injection-attempts]
          (is (nil? (auth/authenticate malicious-username "any-password"))
              (str "Should handle SQL injection: " malicious-username)))))))

(deftest function-signature-consistency-test
  (testing "Function signatures match expectations"
    ;; Verify arity
    (testing "authenticate accepts exactly 2 arguments"
      (let [arglists (:arglists (meta #'auth/authenticate))]
        (is arglists "Should have arglists metadata")
        (is (= 1 (count arglists)) "Should have one arity")
        (is (= 2 (count (first arglists))) "Should accept 2 arguments")))))

(deftest error-handling-documentation-test
  (testing "Error handling matches documentation"
    ;; Create user for testing
    (user/create-user! {:username "error-test" :password "test123" :name "Error Test"})
    
    (testing "does not throw exceptions for invalid inputs"
      ;; These should return nil, not throw
      (is (nil? (auth/authenticate nil nil)) "nil/nil → nil")
      (is (nil? (auth/authenticate "" "")) "empty/empty → nil")
      (is (nil? (auth/authenticate "error-test" nil)) "valid/nil → nil")
      (is (nil? (auth/authenticate nil "test123")) "nil/valid → nil"))
    
    (testing "handles extremely long inputs without error"
      (let [long-str (apply str (repeat 10000 "x"))]
        (is (nil? (auth/authenticate long-str "password")) "long username → nil")
        (is (nil? (auth/authenticate "error-test" long-str)) "long password → nil")))
    
    (testing "handles special characters in inputs"
      (let [special-chars "!@#$%^&*()_+-=[]{}|;:,.<>?/`~\"'\\"]
        (is (nil? (auth/authenticate special-chars "password")) "special username → nil")
        (is (nil? (auth/authenticate "error-test" special-chars)) "special password → nil")))))

(deftest performance-characteristics-documentation-test
  (testing "Performance characteristics match expectations"
    ;; Create test user
    (user/create-user! {:username "perf-test" :password "performance-password-123" :name "Performance"})
    
    (testing "authentication completes in reasonable time"
      (let [iterations 100
            times (repeatedly iterations
                     #(let [start (System/nanoTime)]
                        (auth/authenticate "perf-test" "performance-password-123")
                        (- (System/nanoTime) start)))
            total-time (reduce + times)
            avg-time-ns (/ total-time iterations)
            avg-time-ms (/ avg-time-ns 1000000.0)]
        
        ;; Documented expectation: < 250ms per authentication (adjusted for test environment)
        (is (< avg-time-ms 250)
            (str "Average authentication should be < 250ms, was " 
                 (format "%.2f" (double avg-time-ms)) "ms"))))
    
    (testing "failed authentication has similar timing to successful"
      (let [iterations 50
            success-times (repeatedly iterations
                           #(let [start (System/nanoTime)]
                              (auth/authenticate "perf-test" "performance-password-123")
                              (- (System/nanoTime) start)))
            fail-times (repeatedly iterations
                         #(let [start (System/nanoTime)]
                            (auth/authenticate "perf-test" "wrong-password")
                            (- (System/nanoTime) start)))
            
            avg-success (/ (reduce + success-times) iterations)
            avg-fail (/ (reduce + fail-times) iterations)
            ratio (/ (max avg-success avg-fail) (min avg-success avg-fail))]
        
        ;; Documented: Timing should not reveal success/failure
        (is (< ratio 2.0)
            (str "Success/failure timing difference should be < 2x, was "
                 (format "%.2f" (double ratio)) "x"))))))

(deftest security-guarantees-documentation-test
  (testing "Security guarantees are maintained"
    ;; Create multiple test users
    (user/create-user! {:username "user1" :password "pass1" :name "User One"})
    (user/create-user! {:username "user2" :password "pass2" :name "User Two"})
    
    (testing "authentication does not leak information about other users"
      ;; Attempt to authenticate user1 with user2's (wrong) password
      (let [result (auth/authenticate "user1" "pass2")]
        ;; Should return nil, not reveal that user2 exists or anything about user2
        (is (nil? result) "Wrong password should return nil")))
    
    (testing "password hashes are never exposed"
      (let [result (auth/authenticate "user1" "pass1")]
        (is (not (contains? result :password_hash)) "password_hash should not be in result")
        (is (nil? (:password_hash result)) "password_hash value should be nil")
        
        ;; Also check that other sensitive fields aren't exposed
        (is (not (contains? result :salt)) "salt should not be in result")
        (is (not (contains? result :created_at)) "created_at should not be in result")))))

(deftest integration-documentation-test
  (testing "Integration behavior matches documentation"
    ;; Documented: Works with user component
    (testing "integrates with user component for user creation"
      (let [username "integration-doc-user"
            password "integration-pass"]
        ;; Create user via user component
        (user/create-user! {:username username :password password :name "Integration"})
        
        ;; Authenticate via auth component
        (let [result (auth/authenticate username password)]
          (is result "Should authenticate successfully")
          (is (= username (:username result)) "Should return correct username")
          (is (= "Integration" (:name result)) "Should return correct name"))))
    
    ;; Documented: Independent of session state
    (testing "does not depend on session state"
      ;; Should work regardless of session
      (let [result1 (auth/authenticate "user1" "pass1")
            result2 (auth/authenticate "user1" "pass1")]
        (is (= result1 result2) "Same credentials should produce same result regardless of session")))))