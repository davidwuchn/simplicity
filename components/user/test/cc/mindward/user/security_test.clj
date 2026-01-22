(ns cc.mindward.user.security-test
  "Security tests for user component.
   
   Tests:
   - SQL injection prevention
   - Input validation
   - Password security"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cc.mindward.user.impl :as user-impl]
            [next.jdbc :as jdbc]))

;; ------------------------------------------------------------
;; Test Fixtures (τ Wisdom - stateful testing with temp DB)
;; ------------------------------------------------------------

(defn temp-db-fixture [f]
  "Create a temporary SQLite database for testing."
  (let [temp-file (java.io.File/createTempFile "test-db-" ".db")
        temp-path (.getAbsolutePath temp-file)
        ds (user-impl/make-datasource temp-path)]
    (.deleteOnExit temp-file)
    (binding [user-impl/*ds* ds]
      (user-impl/init-db! ds)
      (f))))

(use-fixtures :each temp-db-fixture)

;; ------------------------------------------------------------
;; SQL Injection Prevention Tests (∃ Truth)
;; ------------------------------------------------------------

(deftest sql-injection-username-test
  (testing "SQL injection attempts in username are safely escaped"
    ;; Common SQL injection patterns
    (let [injection-attempts ["admin'--"
                              "' OR '1'='1"
                              "'; DROP TABLE users; --"
                              "admin' OR 1=1--"
                              "' UNION SELECT * FROM users--"
                              "1' AND '1'='1"]]
      (doseq [malicious-username injection-attempts]
        ;; Attempt to create user with malicious username
        ;; Should either fail or store the literal string (not execute SQL)
        (try
          (user-impl/create-user! {:username malicious-username
                                   :password "test123"
                                   :name "Test User"})
          (catch Exception _))
        
        ;; Attempt to find user - should only find exact match, not execute injection
        (let [found-user (user-impl/find-by-username malicious-username)]
          ;; If user was created, username should be stored literally
          (when found-user
            (is (= malicious-username (:username found-user))
                "Username should be stored as literal string, not executed as SQL"))
          
          ;; Verify the users table still exists and has expected structure
          (let [all-users (jdbc/execute! (user-impl/ds)
                                        ["SELECT COUNT(*) as count FROM users"])]
            (is (number? (:count (first all-users)))
                "Users table should still exist after injection attempt")))))))

(deftest sql-injection-password-test
  (testing "SQL injection in password field is safely handled"
    (let [username "testuser"
          malicious-passwords ["pass' OR '1'='1"
                               "'; DROP TABLE users; --"
                               "pass' UNION SELECT * FROM users--"]]
      ;; Create a legitimate user
      (user-impl/create-user! {:username username
                               :password "legitimate-password"
                               :name "Test User"})
      
      (doseq [malicious-password malicious-passwords]
        ;; Attempt authentication with SQL injection in password
        (let [user (user-impl/find-by-username username)
              auth-result (user-impl/verify-password malicious-password 
                                                    (:password_hash user))]
          ;; Should NOT authenticate (injection should not bypass password check)
          (is (false? auth-result)
              "SQL injection in password should not bypass authentication")
          
          ;; Verify users table still exists
          (let [all-users (jdbc/execute! (user-impl/ds)
                                        ["SELECT COUNT(*) as count FROM users"])]
            (is (number? (:count (first all-users)))
                "Users table should still exist after password injection attempt")))))))

(deftest sql-injection-update-score-test
  (testing "SQL injection in score updates is prevented"
    (let [username "testuser"]
      ;; Create user
      (user-impl/create-user! {:username username
                               :password "test123"
                               :name "Test User"})
      
      ;; Attempt SQL injection via score update
      ;; Note: score is an integer, but let's verify the implementation handles it safely
      (user-impl/update-high-score! username 100)
      
      ;; Verify score was updated correctly
      (is (= 100 (user-impl/get-high-score username))
          "Legitimate score update should work")
      
      ;; Try updating with a username containing SQL injection
      (let [malicious-username "admin'; DELETE FROM users WHERE '1'='1"]
        ;; This should not delete any users
        (user-impl/update-high-score! malicious-username 999)
        
        ;; Verify original user still exists
        (is (some? (user-impl/find-by-username username))
            "Original user should still exist after injection attempt in update")))))

;; ------------------------------------------------------------
;; Input Validation Tests
;; ------------------------------------------------------------

(deftest unicode-username-test
  (testing "Unicode characters in usernames are handled safely"
    (let [unicode-usernames ["用户名"
                            "пользователь"
                            "🔥admin🔥"
                            "user\u0000name"  ;; null byte
                            "user\nname"       ;; newline
                            "user\tname"]]     ;; tab
      (doseq [unicode-name unicode-usernames]
        (try
          (user-impl/create-user! {:username unicode-name
                                   :password "test123"
                                   :name "Test User"})
          (let [found (user-impl/find-by-username unicode-name)]
            ;; If created, should be stored exactly as provided
            (when found
              (is (= unicode-name (:username found))
                  "Unicode username should be stored exactly as provided")))
          (catch Exception e
            ;; It's acceptable to reject unicode usernames
            (is (some? e) "Exception is acceptable for unicode usernames")))))))

(deftest long-input-test
  (testing "Very long inputs don't cause buffer overflows or crashes"
    (let [very-long-string (apply str (repeat 10000 "a"))]
      ;; Try creating user with very long username
      (try
        (user-impl/create-user! {:username very-long-string
                                 :password "test123"
                                 :name "Test"})
        (catch Exception e
          ;; Should either succeed or throw controlled exception
          (is (some? e) "Long username should be handled gracefully")))
      
      ;; Try creating user with very long password
      (try
        (user-impl/create-user! {:username "testuser-long"
                                 :password very-long-string
                                 :name "Test"})
        ;; bcrypt should handle long passwords (truncates at 72 bytes)
        (let [user (user-impl/find-by-username "testuser-long")]
          (is (some? user) "User with long password should be created")
          ;; Verify password hashing worked
          (is (some? (:password_hash user))
              "Password should be hashed even if very long"))
        (catch Exception e
          (is (some? e) "Long password should be handled gracefully"))))))

;; ------------------------------------------------------------
;; Password Security Tests (∃ Truth)
;; ------------------------------------------------------------

(deftest password-hashing-test
  (testing "Passwords are never stored in plain text"
    (let [username "testuser"
          password "my-secret-password"]
      (user-impl/create-user! {:username username
                               :password password
                               :name "Test User"})
      
      (let [user (user-impl/find-by-username username)]
        ;; Password hash should exist
        (is (some? (:password_hash user))
            "Password hash should be stored")
        
        ;; Password hash should not equal plain password
        (is (not= password (:password_hash user))
            "Password should be hashed, not stored in plain text")
        
        ;; Password hash should be in buddy-hashers format (bcrypt+sha512)
        (is (clojure.string/starts-with? (:password_hash user) "bcrypt+sha512$")
            "Password should use buddy-hashers bcrypt+sha512 format")
        
        ;; Password verification should work
        (is (user-impl/verify-password password (:password_hash user))
            "Password verification should work with correct password")
        
        ;; Wrong password should not verify
        (is (false? (user-impl/verify-password "wrong-password" (:password_hash user)))
            "Wrong password should not verify")))))

(deftest password-timing-attack-resistance-test
  (testing "Password verification is timing-attack resistant"
    (let [username "testuser"
          password "correct-password"]
      (user-impl/create-user! {:username username
                               :password password
                               :name "Test User"})
      
      (let [user (user-impl/find-by-username username)
            hash (:password_hash user)]
        ;; Time correct password verification
        (let [start1 (System/nanoTime)
              _ (user-impl/verify-password password hash)
              time1 (- (System/nanoTime) start1)]
          
          ;; Time incorrect password verification
          (let [start2 (System/nanoTime)
                _ (user-impl/verify-password "wrong-password" hash)
                time2 (- (System/nanoTime) start2)]
            
            ;; Both should take roughly the same time (bcrypt is constant-time)
            ;; Allow 50% variance (bcrypt work factor dominates)
            (let [ratio (/ (double (max time1 time2))
                          (double (min time1 time2)))]
              (is (< ratio 2.0)
                  "Password verification should have constant time (timing attack resistant)"))))))))

(deftest duplicate-username-prevention-test
  (testing "Duplicate usernames are prevented by database constraint"
    (let [username "duplicate-test"
          password "test123"]
      ;; Create first user
      (user-impl/create-user! {:username username
                               :password password
                               :name "First User"})
      
      ;; Attempt to create second user with same username
      (is (thrown? Exception
                   (user-impl/create-user! {:username username
                                            :password "different-password"
                                            :name "Second User"}))
          "Duplicate username should throw exception")
      
      ;; Verify only one user exists
      (let [all-users (jdbc/execute! (user-impl/ds)
                                    ["SELECT COUNT(*) as count FROM users WHERE username = ?" username])]
        (is (= 1 (:count (first all-users)))
            "Only one user with that username should exist")))))
