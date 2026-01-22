(ns cc.mindward.ui.components-test
  "Basic sanity tests for UI components.
   
   Verifies that components return valid Hiccup structures."
  (:require [clojure.test :refer [deftest is testing]]
            [cc.mindward.ui.components :as c]))

(deftest alert-test
  (testing "Alert component returns valid Hiccup"
    (let [result (c/alert {:type :error :message "Test"})]
      (is (vector? result) "Should return a vector")
      (is (= :div (first result)) "Should be a div element")
      (is (some #(= "Test" %) (flatten result)) "Should contain the message"))))

(deftest form-input-test
  (testing "Form input returns valid Hiccup"
    (let [result (c/form-input {:id "test" :name "test" :label "Label"})]
      (is (vector? result))
      (is (some #(= "Label" %) (flatten result))))))

(deftest button-test
  (testing "Button returns valid Hiccup"
    (let [result (c/button {:text "Click"})]
      (is (vector? result))
      (is (= :button (first result)))
      (is (some #(= "Click" %) (flatten result))))))

(deftest link-button-test
  (testing "Link button returns valid Hiccup"
    (let [result (c/link-button {:href "/test" :text "Link"})]
      (is (vector? result))
      (is (= :a (first result)))
      (is (some #(= "Link" %) (flatten result))))))

(deftest card-test
  (testing "Card returns valid Hiccup"
    (let [result (c/card {:title "Title" :content [:p "Content"]})]
      (is (vector? result))
      (is (= :div (first result)))
      (is (some #(= "Title" %) (flatten result)))
      (is (some #(= "Content" %) (flatten result))))))

(deftest nav-link-test
  (testing "Nav link returns valid Hiccup"
    (let [result (c/nav-link {:href "/test" :text "Link"})]
      (is (vector? result))
      (is (= :a (first result)))
      (is (some #(= "Link" %) (flatten result))))))

(deftest table-test
  (testing "Table returns valid Hiccup with data"
    (let [result (c/table {:columns ["Name" "Score"]
                           :rows [{:cells ["Alice" "100"]}]})]
      (is (vector? result))
      (is (some #(= "Name" %) (flatten result)))
      (is (some #(= "Alice" %) (flatten result)))
      (is (some #(= "100" %) (flatten result))))))

(deftest badge-test
  (testing "Badge returns valid Hiccup"
    (let [result (c/badge {:text "New"})]
      (is (vector? result))
      (is (= :span (first result)))
      (is (some #(= "New" %) (flatten result))))))

(deftest spinner-test
  (testing "Spinner returns valid Hiccup"
    (let [result (c/spinner {})]
      (is (vector? result))
      (is (= :div (first result))))))
