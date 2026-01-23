(ns cc.mindward.auth.interface-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cc.mindward.auth.interface :as auth]
            [cc.mindward.user.interface :as user]
            [cc.mindward.user.impl :as user-impl]))

;; Integration test fixture: use real temporary database
;; (∃ Truth): Test against actual DB instead of mocks to verify real behavior
(defn test-db-fixture
  "Fixture that creates a temporary SQLite database for each test.
   Uses dynamic binding to inject the test datasource."
  [f]
  (let [temp-file (java.io.File/createTempFile "test_auth_" ".db")
        db-path (.getAbsolutePath temp-file)
        test-ds (user-impl/make-datasource db-path)]
    (try
      (binding [user-impl/*ds* test-ds]
        (user-impl/init-db! test-ds)
        (f))
      (finally
        (.delete temp-file)))))

(use-fixtures :each test-db-fixture)

(deftest authenticate-integration-test
  (testing "Integration test: auth component with real database"
    ;; Create a real user in the test database
    (user/create-user! {:username "testuser" :password "secret" :name "Test User"})
    
    (testing "valid credentials returns user without password hash"
      (let [result (auth/authenticate "testuser" "secret")]
        (is result)
        (is (= "testuser" (:username result)))
        (is (= "Test User" (:name result)))
        (is (nil? (:password_hash result)) "password hash should be stripped")))

    (testing "invalid password returns nil"
      (is (not (auth/authenticate "testuser" "wrong"))))

    (testing "unknown user returns nil"
      (is (not (auth/authenticate "unknown" "secret"))))

    (testing "nil username returns nil"
      (is (nil? (auth/authenticate nil "password"))))

    (testing "empty username returns nil"
      (is (nil? (auth/authenticate "" "password"))))

    (testing "nil password returns nil"
      (is (nil? (auth/authenticate "testuser" nil))))

    (testing "empty password returns nil"
      (is (nil? (auth/authenticate "testuser" ""))))

    (testing "SQL injection attempts should not break authentication"
      (is (nil? (auth/authenticate "'; DROP TABLE users; --" "password")))
      (is (nil? (auth/authenticate "testuser" "'; DROP TABLE users; --"))))

    (testing "very long credentials should be handled gracefully"
      (let [long-string (apply str (repeat 1000 "a"))]
        (is (nil? (auth/authenticate long-string "password")))
        (is (nil? (auth/authenticate "testuser" long-string)))))))

(deftest authenticate-password-verification-test
  (testing "Password verification against real bcrypt hashes"
    (user/create-user! {:username "secure-user" :password "my-password" :name "Secure"})
    
    (testing "correct password authenticates successfully"
      (let [result (auth/authenticate "secure-user" "my-password")]
        (is result)
        (is (= "secure-user" (:username result)))))
    
    (testing "incorrect password fails"
      (is (nil? (auth/authenticate "secure-user" "wrong-password"))))
    
    (testing "password with special characters works"
      (user/create-user! {:username "special" :password "p@ssw0rd!#$%" :name "Special"})
      (let [result (auth/authenticate "special" "p@ssw0rd!#$%")]
        (is result)
        (is (= "special" (:username result)))))
    
    (testing "unicode password works"
      (user/create-user! {:username "unicode" :password "密码123" :name "Unicode"})
      (let [result (auth/authenticate "unicode" "密码123")]
        (is result)
        (is (= "unicode" (:username result)))))))

(deftest authenticate-security-test
  (testing "Security: password hash never exposed in auth result"
    (user/create-user! {:username "player" :password "game123" :name "Player"})
    
    (let [result (auth/authenticate "player" "game123")]
      (is (contains? result :username))
      (is (contains? result :name))
      (is (not (contains? result :password_hash)) "password_hash key should not exist")
      (is (nil? (:password_hash result)) "password_hash value should be nil"))))
