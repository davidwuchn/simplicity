(ns cc.mindward.user.interface-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cc.mindward.user.interface :as user]
            [cc.mindward.user.impl :as impl]
            [next.jdbc :as jdbc]))

(defn test-db-fixture [f]
  (let [temp-file (java.io.File/createTempFile "test_simplicity_" ".db")
        db-path (.getAbsolutePath temp-file)
        ds (jdbc/get-datasource {:dbtype "sqlite" :dbname db-path})]
    (try
      (with-redefs [impl/ds ds]
        (impl/init-db!)
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
      (is (= 0 (:high_score u))))))

(deftest update-high-score-test
  (testing "update high score"
    (user/create-user! {:username "player2" :password "pass" :name "Player Two"})
    (user/update-high-score! "player2" 100)
    (is (= 100 (user/get-high-score "player2")))
    ;; Should verify it only updates if higher
    (user/update-high-score! "player2" 50)
    (is (= 100 (user/get-high-score "player2")))
    (user/update-high-score! "player2" 150)
    (is (= 150 (user/get-high-score "player2")))))

(deftest leaderboard-test
  (testing "leaderboard"
    (user/create-user! {:username "a" :password "p" :name "A"})
    (user/create-user! {:username "b" :password "p" :name "B"})
    (user/create-user! {:username "c" :password "p" :name "C"})
    
    (user/update-high-score! "a" 100)
    (user/update-high-score! "b" 200)
    (user/update-high-score! "c" 50)
    
    (let [board (user/get-leaderboard)]
      ;; Note: admin user is created by init-db!
      (is (>= (count board) 3))
      
      ;; Verify ordering
      (let [top-user (first board)
            second-user (second board)]
        (is (= "b" (:username top-user)))
        (is (= 200 (:high_score top-user)))
        (is (= "a" (:username second-user)))
        (is (= 100 (:high_score second-user)))))))
