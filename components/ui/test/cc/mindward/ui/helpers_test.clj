(ns cc.mindward.ui.helpers-test
  "Tests for UI helper functions.
   
   Verifies that helper functions:
   - Process inputs correctly
   - Return expected outputs
   - Handle edge cases
   - Validate data properly"
  (:require [clojure.test :refer [deftest is testing]]
            [cc.mindward.ui.helpers :as h]))

;; === Class Helpers Tests ===

(deftest classes-test
  (testing "classes function"
    (testing "joins non-nil strings"
      (is (= "foo bar baz" (h/classes "foo" "bar" "baz"))))
    
    (testing "filters out nil values"
      (is (= "foo baz" (h/classes "foo" nil "baz"))))
    
    (testing "handles empty input"
      (is (= "" (h/classes))))
    
    (testing "handles all nil input"
      (is (= "" (h/classes nil nil nil))))))

(deftest conditional-class-test
  (testing "conditional-class function"
    (testing "returns class when condition is true"
      (is (= "active" (h/conditional-class true "active"))))
    
    (testing "returns nil when condition is false"
      (is (nil? (h/conditional-class false "active"))))
    
    (testing "handles truthy values"
      (is (= "active" (h/conditional-class 1 "active"))))
    
    (testing "handles falsy values"
      (is (nil? (h/conditional-class nil "active"))))))

(deftest merge-attrs-test
  (testing "merge-attrs function"
    (testing "merges class attributes"
      (is (= {:class "foo bar"}
             (h/merge-attrs {:class "foo"} {:class "bar"}))))
    
    (testing "preserves other attributes"
      (is (= {:id "test" :class "foo bar"}
             (h/merge-attrs {:id "test" :class "foo"} {:class "bar"}))))
    
    (testing "handles nil classes"
      (is (= {:class "foo"}
             (h/merge-attrs {:class "foo"} {:class nil}))))
    
    (testing "handles empty input"
      (is (= {} (h/merge-attrs {} {}))))))

;; === Form Helpers Tests ===

(deftest form-errors-test
  (testing "form-errors function"
    (testing "extracts error fields from params"
      (is (= {:error "General error" :username-error "Invalid"}
             (h/form-errors {:error "General error"
                             :username-error "Invalid"
                             :other "data"}))))
    
    (testing "returns empty map when no errors"
      (is (= {} (h/form-errors {:username "john" :password "pass"}))))
    
    (testing "handles nil params"
      (is (= {} (h/form-errors nil))))
    
    (testing "handles empty params"
      (is (= {} (h/form-errors {}))))))

(deftest has-error?-test
  (testing "has-error? function"
    (testing "returns true when error exists"
      (is (true? (h/has-error? {:username-error "Error"} :username))))
    
    (testing "returns false when no error"
      (is (false? (h/has-error? {:password-error "Error"} :username))))
    
    (testing "returns false for nil errors"
      (is (false? (h/has-error? nil :username))))
    
    (testing "returns false for empty errors"
      (is (false? (h/has-error? {} :username))))))

(deftest error-message-test
  (testing "error-message function"
    (testing "returns error message for field"
      (is (= "Invalid input"
             (h/error-message {:username-error "Invalid input"} :username))))
    
    (testing "returns nil when no error"
      (is (nil? (h/error-message {:password-error "Bad"} :username))))
    
    (testing "handles nil params"
      (is (nil? (h/error-message nil :username))))
    
    (testing "handles empty params"
      (is (nil? (h/error-message {} :username))))))

;; === Text Helpers Tests ===

(deftest truncate-test
  (testing "truncate function"
    (testing "truncates long strings"
      (is (= "Hello..." (h/truncate "Hello world this is long" 5))))
    
    (testing "does not truncate short strings"
      (is (= "Hi" (h/truncate "Hi" 5))))
    
    (testing "handles exact length"
      (is (= "Hello" (h/truncate "Hello" 5))))
    
    (testing "handles empty string"
      (is (= "" (h/truncate "" 5))))))

(deftest pluralize-test
  (testing "pluralize function"
    (testing "returns singular for 1"
      (is (= "item" (h/pluralize 1 "item" "items"))))
    
    (testing "returns plural for 0"
      (is (= "items" (h/pluralize 0 "item" "items"))))
    
    (testing "returns plural for 2+"
      (is (= "items" (h/pluralize 5 "item" "items"))))
    
    (testing "handles negative numbers"
      (is (= "items" (h/pluralize -1 "item" "items"))))))

(deftest format-number-test
  (testing "format-number function"
    (testing "formats small numbers unchanged"
      (is (= "999" (h/format-number 999))))
    
    (testing "formats thousands with comma"
      (is (= "1,000" (h/format-number 1000)))
      (is (= "10,500" (h/format-number 10500))))
    
    (testing "formats millions"
      (is (= "1,000,000" (h/format-number 1000000))))
    
    (testing "handles zero"
      (is (= "0" (h/format-number 0))))
    
    (testing "handles negative numbers"
      (is (= "-1,000" (h/format-number -1000))))))

;; === Validation Helpers Tests ===

(deftest valid-username?-test
  (testing "valid-username? function"
    (testing "accepts valid usernames"
      (is (boolean (h/valid-username? "user123")))
      (is (boolean (h/valid-username? "test_user")))
      (is (boolean (h/valid-username? "admin-01"))))
    
    (testing "rejects too short"
      (is (not (h/valid-username? "ab"))))
    
    (testing "rejects too long"
      (is (not (h/valid-username? "thisusernameiswaytoolong123"))))
    
    (testing "rejects invalid characters"
      (is (not (h/valid-username? "user@name")))
      (is (not (h/valid-username? "user name")))
      (is (not (h/valid-username? "user.name"))))
    
    (testing "rejects empty string"
      (is (not (h/valid-username? ""))))))

(deftest valid-password?-test
  (testing "valid-password? function"
    (testing "accepts valid passwords"
      (is (boolean (h/valid-password? "password123")))
      (is (boolean (h/valid-password? "mySecretPass!"))))
    
    (testing "rejects too short"
      (is (not (h/valid-password? "pass123")))
      (is (not (h/valid-password? "1234567"))))
    
    (testing "accepts exactly 8 characters"
      (is (boolean (h/valid-password? "12345678"))))
    
    (testing "rejects empty string"
      (is (not (h/valid-password? ""))))))

(deftest valid-name?-test
  (testing "valid-name? function"
    (testing "accepts valid names"
      (is (boolean (h/valid-name? "John Doe")))
      (is (boolean (h/valid-name? "Alice")))
      (is (boolean (h/valid-name? "Bob-Smith Jr."))))
    
    (testing "rejects too short"
      (is (not (h/valid-name? "Al"))))
    
    (testing "rejects too long"
      (is (not (h/valid-name? (apply str (repeat 51 "a"))))))
    
    (testing "accepts exactly 3 and 50 characters"
      (is (boolean (h/valid-name? "Bob")))
      (is (boolean (h/valid-name? (apply str (repeat 50 "a"))))))
    
    (testing "rejects empty string"
      (is (not (h/valid-name? ""))))))

;; === Session Helpers Tests ===

(deftest logged-in?-test
  (testing "logged-in? function"
    (testing "returns true when user in session"
      (is (true? (h/logged-in? {:user {:id 1 :username "test"}}))))
    
    (testing "returns false when no user"
      (is (false? (h/logged-in? {}))))
    
    (testing "returns false for nil session"
      (is (false? (h/logged-in? nil))))))

(deftest current-user-test
  (testing "current-user function"
    (testing "returns user from session"
      (is (= {:id 1 :username "test"}
             (h/current-user {:user {:id 1 :username "test"}}))))
    
    (testing "returns nil when no user"
      (is (nil? (h/current-user {}))))
    
    (testing "returns nil for nil session"
      (is (nil? (h/current-user nil))))))

(deftest username-test
  (testing "username function"
    (testing "returns username from session"
      (is (= "testuser"
             (h/username {:user {:username "testuser"}}))))
    
    (testing "returns nil when no user"
      (is (nil? (h/username {}))))
    
    (testing "returns nil for nil session"
      (is (nil? (h/username nil))))))

;; === URL Helpers Tests ===

(deftest query-params-test
  (testing "query-params function"
    (testing "builds query string from single parameter"
      (is (= "page=1"
             (h/query-params {:page 1}))))
    
    (testing "builds query string from multiple parameters"
      (let [result (h/query-params {:page 1 :sort "name"})]
        (is (re-find #"page=1" result))
        (is (re-find #"sort=name" result))))
    
    (testing "handles URL encoding"
      (is (re-find #"hello\+world"
                   (h/query-params {:q "hello world"}))))
    
    (testing "handles empty map"
      (is (= "" (h/query-params {}))))))

(deftest url-with-params-test
  (testing "url-with-params function"
    (testing "adds single parameter"
      (is (= "/page?foo=bar"
             (h/url-with-params "/page" {:foo "bar"}))))
    
    (testing "adds multiple parameters"
      (is (re-find #"foo=bar"
                   (h/url-with-params "/page" {:foo "bar" :baz "qux"})))
      (is (re-find #"baz=qux"
                   (h/url-with-params "/page" {:foo "bar" :baz "qux"}))))
    
    (testing "handles empty params"
      (is (= "/page" (h/url-with-params "/page" {}))))
    
    (testing "encodes special characters"
      (is (re-find #"hello\+world"
                   (h/url-with-params "/search" {:q "hello world"}))))))

;; === JS Helpers Tests ===

(deftest js-safe-string-test
  (testing "js-safe-string function"
    (testing "escapes single quotes"
      (is (= "don\\'t" (h/js-safe-string "don't"))))
    
    (testing "escapes backslashes"
      (is (= "path\\\\to\\\\file" (h/js-safe-string "path\\to\\file"))))
    
    (testing "escapes newlines"
      (is (= "line1\\nline2" (h/js-safe-string "line1\nline2"))))
    
    (testing "handles safe strings unchanged"
      (is (= "safe string" (h/js-safe-string "safe string"))))))

(deftest js-object-test
  (testing "js-object function"
    (testing "converts map to JS object notation"
      (is (= "{foo: 'bar'}"
             (h/js-object {:foo "bar"}))))
    
    (testing "handles multiple keys"
      (let [result (h/js-object {:foo "bar" :baz 123})]
        (is (re-find #"foo: 'bar'" result))
        (is (re-find #"baz: 123" result))))
    
    (testing "handles numbers without quotes"
      (is (= "{count: 42}"
             (h/js-object {:count 42}))))
    
    (testing "handles empty map"
      (is (= "{}" (h/js-object {}))))))
