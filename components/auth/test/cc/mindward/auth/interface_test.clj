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
        (is (not (auth/authenticate "unknown" "secret")))))))
