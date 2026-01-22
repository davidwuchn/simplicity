(ns cc.mindward.auth.interface-test
  (:require [clojure.test :refer [deftest is testing]]
            [cc.mindward.auth.interface :as auth]
            [cc.mindward.user.interface :as user]))

(deftest authenticate-test
  (with-redefs [user/find-by-username (fn [u] (when (= u "testuser")
                                              {:username "testuser" :password "secret"}))]
    (testing "valid credentials"
      (is (auth/authenticate "testuser" "secret")))

    (testing "invalid password"
      (is (not (auth/authenticate "testuser" "wrong"))))

    (testing "unknown user"
      (is (not (auth/authenticate "unknown" "secret"))))))
