(ns cc.mindward.user.interface-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cc.mindward.user.interface :as user]
            [cc.mindward.user.impl :as impl]))

(defn test-db-fixture
  "Fixture that creates a temporary SQLite database for each test.
   Uses dynamic binding to inject the test datasource.
   
   Note: Uses non-pooled datasource for faster test execution."
  [f]
  (let [temp-file (java.io.File/createTempFile "test_simplicity_" ".db")
        db-path (.getAbsolutePath temp-file)
        ;; Use non-pooled datasource for tests (faster, no connection overhead)
        test-ds (impl/make-datasource db-path {:pool? false})]
    (try
      (binding [impl/*ds* test-ds]
        (impl/init-db! test-ds)
        (f))
      (finally
        (.delete temp-file)))))

(use-fixtures :each test-db-fixture)

(deftest create-and-find-user-test
  (testing "create and find user"
    (user/create-user! {:username "player1" :password "pass" :name "Player One"})
    (let [u (user/find-by-username "player1")]
      (is (= "player1" (:username u)))
      (is (= "Player One" (:name u)))
      (is (= 0 (:high_score u)))
      (is (:password_hash u) "password should be hashed and stored"))))

(deftest password-hashing-test
  (testing "passwords are hashed, not stored plain"
    (user/create-user! {:username "secure-user" :password "my-secret" :name "Secure"})
    (let [u (user/find-by-username "secure-user")]
      (is (not= "my-secret" (:password_hash u)) "password should not be stored plain")
      (is (user/verify-password "my-secret" (:password_hash u)) "correct password verifies")
      (is (not (user/verify-password "wrong-password" (:password_hash u))) "wrong password fails"))))

(deftest update-high-score-test
  (testing "update high score only if higher"
    (user/create-user! {:username "player2" :password "pass" :name "Player Two"})
    (user/update-high-score! "player2" 100)
    (is (= 100 (user/get-high-score "player2")))
    
    ;; Should NOT update if lower (SQL MAX behavior)
    (user/update-high-score! "player2" 50)
    (is (= 100 (user/get-high-score "player2")) "score should remain at 100")
    
    ;; Should update if higher
    (user/update-high-score! "player2" 150)
    (is (= 150 (user/get-high-score "player2")))))

(deftest leaderboard-test
  (testing "leaderboard returns users ordered by high score descending"
    (user/create-user! {:username "a" :password "p" :name "A"})
    (user/create-user! {:username "b" :password "p" :name "B"})
    (user/create-user! {:username "c" :password "p" :name "C"})
    
    (user/update-high-score! "a" 100)
    (user/update-high-score! "b" 200)
    (user/update-high-score! "c" 50)
    
    (let [board (user/get-leaderboard)]
      (is (= 3 (count board)))
      
      ;; Verify ordering (highest first)
      (is (= ["b" "a" "c"] (mapv :username board)))
      (is (= [200 100 50] (mapv :high_score board))))))
