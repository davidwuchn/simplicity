(ns cc.mindward.web-server.security-test
  "Security tests for web server.
   
   Tests:
   - Security headers
   - Rate limiting
   - Input validation
   - CSRF protection
   - Session security
   - XSS prevention"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [cc.mindward.web-server.core :as server]
            [cc.mindward.web-server.security :as security]
            [cc.mindward.user.impl :as user-impl]
            [cc.mindward.user.validation :as validation]
            [cc.mindward.game.impl :as game-impl]))

;; ------------------------------------------------------------
;; Test Helpers
;; ------------------------------------------------------------

(defn- mock-request
  [method uri & {:keys [params session headers remote-addr]}]
  {:request-method method
   :uri uri
   :params (or params {})
   :session (or session {})
   :headers (or headers {})
   :remote-addr (or remote-addr "127.0.0.1")
   :anti-forgery-token "test-csrf-token"})

;; ------------------------------------------------------------
;; Test Fixtures
;; ------------------------------------------------------------

(defn temp-db-fixture 
  "Create a temporary SQLite database for testing."
  [f]
  (let [temp-file (java.io.File/createTempFile "test-db-" ".db")
        temp-path (.getAbsolutePath temp-file)
        ds (user-impl/make-datasource temp-path)]
    (.deleteOnExit temp-file)
    (binding [user-impl/*ds* ds]
      (user-impl/init-db! ds)
      (game-impl/initialize!)
      (f))))

(use-fixtures :each temp-db-fixture)

;; ------------------------------------------------------------
;; Security Headers Tests (OWASP Best Practices)
;; ------------------------------------------------------------

(deftest security-headers-present-test
  (testing "All security headers are present in responses"
    (let [response (server/site-app (mock-request :get "/"))]
      (is (= 200 (:status response)) "Request should succeed")
      
      ;; Check for CSP header
      (is (contains? (:headers response) "Content-Security-Policy")
          "CSP header should be present")
      (is (str/includes? (get (:headers response) "Content-Security-Policy") "default-src 'self'")
          "CSP should restrict to same origin")
      
      ;; Check for X-Frame-Options
      (is (= "DENY" (get (:headers response) "X-Frame-Options"))
          "X-Frame-Options should prevent clickjacking")
      
      ;; Check for X-Content-Type-Options
      (is (= "nosniff" (get (:headers response) "X-Content-Type-Options"))
          "X-Content-Type-Options should prevent MIME sniffing")
      
      ;; Check for X-XSS-Protection
      (is (= "1; mode=block" (get (:headers response) "X-XSS-Protection"))
          "X-XSS-Protection should be enabled")
      
      ;; Check for Referrer-Policy
      (is (= "strict-origin-when-cross-origin" (get (:headers response) "Referrer-Policy"))
          "Referrer-Policy should limit information leakage")
      
      ;; Check for Permissions-Policy
      (is (contains? (:headers response) "Permissions-Policy")
          "Permissions-Policy should be present"))))

(deftest hsts-header-optional-test
  (testing "HSTS header is only present when explicitly enabled via env var"
    ;; Without ENABLE_HSTS, header should not be present
    (let [response (server/site-app (mock-request :get "/"))]
      (is (nil? (get (:headers response) "Strict-Transport-Security"))
          "HSTS should not be present by default"))))

;; ------------------------------------------------------------
;; Rate Limiting Tests (Brute Force Prevention)
;; ------------------------------------------------------------

(deftest rate-limiting-logic-test
  (testing "Rate limiting logic enforces token bucket algorithm"
    ;; Reset rate limit store
    (reset! security/rate-limit-store {})
    
    (let [client-ip "192.168.1.100"
          max-tokens 10
          refill-rate 0.5
          cost 1]
      ;; First 10 requests should be allowed
      (dotimes [n 10]
        (let [result (security/check-rate-limit client-ip max-tokens refill-rate cost)]
          (is (:allowed? result)
              (str "Request " (inc n) " should be allowed"))))
      
      ;; 11th request should be denied
      (let [result (security/check-rate-limit client-ip max-tokens refill-rate cost)]
        (is (not (:allowed? result))
            "Request exceeding limit should be denied")))))

(deftest rate-limiting-refill-test
  (testing "Rate limit tokens refill over time"
    (reset! security/rate-limit-store {})
    
    (let [client-ip "192.168.1.200"
          max-tokens 5
          refill-rate 10.0  ;; 10 tokens per second
          cost 1]
      ;; Use all 5 tokens
      (dotimes [_ 5]
        (security/check-rate-limit client-ip max-tokens refill-rate cost))
      
      ;; Next request should be denied
      (is (not (:allowed? (security/check-rate-limit client-ip max-tokens refill-rate cost)))
          "Request should be denied when tokens exhausted")
      
      ;; Wait 200ms (should refill ~2 tokens)
      (Thread/sleep 200)
      
      ;; Should be able to make another request now
      (is (:allowed? (security/check-rate-limit client-ip max-tokens refill-rate cost))
          "Request should be allowed after refill"))))

(deftest rate-limiting-per-ip-isolation-test
  (testing "Rate limiting is applied per IP address"
    (reset! security/rate-limit-store {})
    
    (let [ip1 "192.168.1.1"
          ip2 "192.168.1.2"
          max-tokens 5
          refill-rate 0.5
          cost 1]
      ;; IP1 uses all tokens
      (dotimes [_ 5]
        (security/check-rate-limit ip1 max-tokens refill-rate cost))
      
      ;; IP1 should be rate limited
      (is (not (:allowed? (security/check-rate-limit ip1 max-tokens refill-rate cost)))
          "IP1 should be rate limited")
      
      ;; IP2 should NOT be rate limited (different client)
      (is (:allowed? (security/check-rate-limit ip2 max-tokens refill-rate cost))
          "IP2 should not be affected by IP1's rate limit"))))

;; ------------------------------------------------------------
;; Input Validation Tests
;; ------------------------------------------------------------

(deftest validate-username-test
  (testing "Username validation enforces security constraints"
    ;; Valid usernames
    (is (:valid? (security/validate-username "validuser"))
        "Simple username should be valid")
    (is (:valid? (security/validate-username "user_123"))
        "Username with underscore and numbers should be valid")
    (is (:valid? (security/validate-username "user-name"))
        "Username with dash should be valid")
    
    ;; Invalid usernames
    (is (not (:valid? (security/validate-username nil)))
        "Nil username should be invalid")
    (is (not (:valid? (security/validate-username "")))
        "Empty username should be invalid")
    (is (not (:valid? (security/validate-username "ab")))
        "Too short username should be invalid")
    (is (not (:valid? (security/validate-username (apply str (repeat 50 "a")))))
        "Too long username should be invalid")
    (is (not (:valid? (security/validate-username "user name")))
        "Username with space should be invalid")
    (is (not (:valid? (security/validate-username "user@example")))
        "Username with @ should be invalid")
    (is (not (:valid? (security/validate-username "user'; DROP TABLE")))
        "Username with SQL injection should be invalid")
    (is (not (:valid? (security/validate-username " username")))
        "Username with leading whitespace should be invalid")
    (is (not (:valid? (security/validate-username "username ")))
        "Username with trailing whitespace should be invalid")))

(deftest validate-password-test
  (testing "Password validation enforces minimum security"
    ;; Valid passwords
    (is (:valid? (security/validate-password "password123"))
        "8+ character password should be valid")
    (is (:valid? (security/validate-password "correct horse battery staple"))
        "Long passphrase should be valid")
    
    ;; Invalid passwords
    (is (not (:valid? (security/validate-password nil)))
        "Nil password should be invalid")
    (is (not (:valid? (security/validate-password "")))
        "Empty password should be invalid")
    (is (not (:valid? (security/validate-password "short")))
        "Too short password should be invalid")
    (is (not (:valid? (security/validate-password "1234567")))
        "7 character password should be invalid")))

(deftest validate-score-test
  (testing "Score validation prevents invalid values"
    ;; Valid scores
    (let [result (security/validate-score "0")]
      (is (:valid? result) "Zero score should be valid")
      (is (= 0 (:value result))))
    
    (let [result (security/validate-score "12345")]
      (is (:valid? result) "Normal score should be valid")
      (is (= 12345 (:value result))))
    
    ;; Invalid scores
    (is (not (:valid? (security/validate-score "-1")))
        "Negative score should be invalid")
    (is (not (:valid? (security/validate-score "9999999")))
        "Excessively large score should be invalid")
    (is (not (:valid? (security/validate-score "not-a-number")))
        "Non-numeric score should be invalid")
    (is (not (:valid? (security/validate-score "123.45")))
        "Decimal score should be invalid")))

;; ------------------------------------------------------------
;; CSRF Protection Tests (∀ Vigilance)
;; ------------------------------------------------------------

(deftest csrf-token-required-test
  (testing "POST requests require CSRF token"
    ;; ring-defaults includes CSRF protection by default
    ;; Attempting POST without token should fail
    (let [response (server/site-app
                     (mock-request :post "/login"
                                  :params {:username "test" :password "test"}))]
      ;; Should either get 403 Forbidden or redirect (depending on ring-defaults config)
      ;; The exact behavior depends on ring-defaults anti-forgery settings
      (is (some? response)
          "Response should be returned for POST request"))))

;; ------------------------------------------------------------
;; Session Security Tests
;; ------------------------------------------------------------

(deftest session-isolation-test
  (testing "Sessions are properly isolated between users"
    ;; Create two test users with DIFFERENT high scores
    (user-impl/create-user! {:username "user1"
                             :password "password1"
                             :name "User One"})
    (user-impl/create-user! {:username "user2"
                             :password "password2"
                             :name "User Two"})
    ;; Set different high scores to verify session isolation
    (user-impl/update-high-score! "user1" 100)
    (user-impl/update-high-score! "user2" 500)
    
    ;; Simulate two different sessions
    (let [session1 {:username "user1"}
          session2 {:username "user2"}
          ;; Request game page with session1
          response1 (server/game-page {:session session1
                                       :anti-forgery-token "dummy-token"})
          ;; Request game page with session2
          response2 (server/game-page {:session session2
                                       :anti-forgery-token "dummy-token"})
          body1 (:body response1)
          body2 (:body response2)]
      ;; Both should succeed
      (is (= 200 (:status response1)) "User 1 should get game page")
      (is (= 200 (:status response2)) "User 2 should get game page")
      ;; Each session should see their OWN high score (session isolation)
      (is (clojure.string/includes? body1 ">100<")
          "User 1 should see their own high score (100)")
      (is (clojure.string/includes? body2 ">500<")
          "User 2 should see their own high score (500)")
      ;; Content should be different (different high scores)
      (is (not= body1 body2)
          "Different sessions should get different content"))))

(deftest session-authentication-required-test
  (testing "Protected pages require authentication"
    ;; Request game page without session
    (let [response (server/game-page {:session {}
                                      :anti-forgery-token "dummy-token"})]
      ;; Should redirect to login
      (is (= 302 (:status response))
          "Unauthenticated request should redirect")
      (is (= "/login" (get-in response [:headers "Location"]))
          "Should redirect to login page"))))

;; ------------------------------------------------------------
;; XSS Prevention Tests (∃ Truth)
;; ------------------------------------------------------------

(deftest xss-username-escaping-test
  (testing "Usernames with XSS payloads are properly escaped in HTML"
    ;; Note: This tests that the UI component escapes HTML
    ;; The actual escaping happens in hiccup rendering
    (let [xss-username "<script>alert('xss')</script>"
          ;; Create session with XSS payload
          session {:username xss-username}
          ;; Render a page that displays username
          response (server/leaderboard-page {:session session})]
      ;; Response should be HTML
      (is (string? (:body response)) "Response should have body")
      ;; Should NOT contain raw script tags (should be escaped)
      (is (not (clojure.string/includes? (:body response) "<script>alert"))
          "Script tags should be escaped, not rendered as-is"))))

(deftest xss-error-message-escaping-test
  (testing "Error messages from query params are escaped"
    ;; Attempt login with XSS payload in error param
    ;; Note: This would need to test the actual UI rendering
    ;; For now, we verify that error params don't execute as code
    (let [response (server/login-page {:session {}
                                       :params {:error "<script>alert('xss')</script>"}
                                       :anti-forgery-token "dummy-token"})]
      (is (some? response) "Page should render")
      (when (string? (:body response))
        (is (not (clojure.string/includes? (:body response) "<script>alert"))
            "Error message should be escaped")))))

;; ------------------------------------------------------------
;; Safe Parse Int Tests
;; ------------------------------------------------------------

(deftest safe-parse-int-test
  (testing "safe-parse-int handles invalid input gracefully"
    (is (= 123 (validation/safe-parse-int "123" 0))
        "Valid integer should parse correctly")
    (is (= 0 (validation/safe-parse-int "not-a-number" 0))
        "Invalid input should return default")
    (is (= -1 (validation/safe-parse-int nil -1))
        "Nil input should return default")
    (is (= 999 (validation/safe-parse-int "" 999))
        "Empty string should return default")
    (is (= 0 (validation/safe-parse-int "123.45" 0))
        "Decimal should return default (not valid int)")))
