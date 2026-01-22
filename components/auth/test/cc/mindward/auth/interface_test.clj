(ns cc.mindward.auth.interface-test
  (:require [clojure.test :refer [deftest is testing]]
            [cc.mindward.auth.interface :as auth]
            [cc.mindward.user.interface :as user]
            [buddy.hashers :as hashers]))

(deftest authenticate-test
  (let [hashed-secret (hashers/derive "secret")]
    (with-redefs [user/find-by-username (fn [u] (when (= u "testuser")
                                                  {:username "testuser" 
                                                   :password_hash hashed-secret
                                                   :name "Test User"}))
                  user/verify-password hashers/check]
      (testing "valid credentials returns user without password hash"
        (let [result (auth/authenticate "testuser" "secret")]
          (is result)
          (is (= "testuser" (:username result)))
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
          (is (nil? (auth/authenticate "testuser" long-string))))))))

(deftest authenticate-edge-cases
  (testing "User component errors propagate correctly"
    (with-redefs [user/find-by-username (fn [& args] (throw (ex-info "DB connection failed" {:reason :connection})))]
       (is (thrown? Exception (auth/authenticate "testuser" "secret"))))))
