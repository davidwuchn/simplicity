(ns cc.mindward.ui.helpers
  "Helper functions for UI rendering.
   
   Contains pure utility functions for common UI patterns."
  (:require [clojure.string :as str]))

;; === Class Name Helpers ===

(defn classes
  "Combine multiple class names, filtering out nils and empty strings.
   
   Examples:
   (classes \"foo\" \"bar\") => \"foo bar\"
   (classes \"foo\" nil \"bar\") => \"foo bar\"
   (classes \"foo\" (when false \"bar\")) => \"foo\""
  [& class-names]
  (->> class-names
       (filter some?)
       (filter (complement empty?))
       (str/join " ")))

(defn conditional-class
  "Add a class name conditionally.
   
   Examples:
   (conditional-class true \"active\") => \"active\"
   (conditional-class false \"active\") => nil"
  [condition class-name]
  (when condition class-name))

;; === Attribute Helpers ===

(defn merge-attrs
  "Merge HTML attributes, with special handling for :class.
   
   Examples:
   (merge-attrs {:class \"foo\"} {:class \"bar\"})
   => {:class \"foo bar\"}
   
   (merge-attrs {:id \"x\" :class \"foo\"} {:class \"bar\" :data-x 1})
   => {:id \"x\" :class \"foo bar\" :data-x 1}"
  [& attr-maps]
  (let [merged (apply merge attr-maps)
        class-names (->> attr-maps
                        (map :class)
                        (filter some?)
                        (apply classes))]
    (if (seq class-names)
      (assoc merged :class class-names)
      merged)))

(defn data-attrs
  "Convert a map to data-* attributes.
   
   Examples:
   (data-attrs {:user-id 123 :active true})
   => {:data-user-id \"123\" :data-active \"true\"}"
  [data-map]
  (into {}
        (map (fn [[k v]]
               [(keyword (str "data-" (name k))) (str v)])
             data-map)))

;; === Form Helpers ===

(defn form-errors
  "Extract form errors from params.
   
   Returns a map of field-name -> error-message."
  [params]
  (select-keys params [:error :username-error :password-error :name-error]))

(defn has-error?
  "Check if a field has an error.
   
   Examples:
   (has-error? {:username-error \"Required\"} :username) => true
   (has-error? {} :username) => false"
  [params field-name]
  (boolean (get params (keyword (str (name field-name) "-error")))))

(defn error-message
  "Get error message for a field.
   
   Examples:
   (error-message {:username-error \"Required\"} :username) => \"Required\"
   (error-message {} :username) => nil"
  [params field-name]
  (get params (keyword (str (name field-name) "-error"))))

;; === Text Helpers ===

(defn truncate
  "Truncate text to a maximum length.
   
   Options:
   - :suffix - String to append if truncated (default: \"...\")
   
   Examples:
   (truncate \"Hello World\" 5) => \"Hello...\"
   (truncate \"Hi\" 5) => \"Hi\""
  ([text max-length] (truncate text max-length {}))
  ([text max-length {:keys [suffix] :or {suffix "..."}}]
   (if (<= (count text) max-length)
     text
     (str (subs text 0 max-length) suffix))))

(defn pluralize
  "Pluralize a word based on count.
   
   Examples:
   (pluralize 0 \"item\") => \"items\"
   (pluralize 1 \"item\") => \"item\"
   (pluralize 2 \"item\") => \"items\"
   (pluralize 2 \"child\" \"children\") => \"children\""
  ([count word]
   (if (= count 1) word (str word "s")))
  ([count singular plural]
   (if (= count 1) singular plural)))

(defn format-number
  "Format a number with thousand separators.
   
   Examples:
   (format-number 1000) => \"1,000\"
   (format-number 1234567) => \"1,234,567\""
  [n]
  (let [s (str n)
        len (count s)]
    (if (<= len 3)
      s
      (let [parts (partition-all 3 (reverse s))]
        (->> parts
             (map (comp clojure.string/join reverse))
             reverse
             (clojure.string/join ","))))))

;; === Validation Helpers ===

(defn valid-username?
  "Check if username is valid (3-20 chars, alphanumeric + - _).
   
   Examples:
   (valid-username? \"user123\") => true
   (valid-username? \"ab\") => false
   (valid-username? \"user@123\") => false"
  [username]
  (and (string? username)
       (>= (count username) 3)
       (<= (count username) 20)
       (re-matches #"[a-zA-Z0-9_-]+" username)))

(defn valid-password?
  "Check if password is valid (min 8 chars).
   
   Examples:
   (valid-password? \"password123\") => true
   (valid-password? \"pass\") => false"
  [password]
  (and (string? password)
       (>= (count password) 8)))

(defn valid-name?
  "Check if display name is valid (3-50 chars).
   
   Examples:
   (valid-name? \"John Doe\") => true
   (valid-name? \"AB\") => false"
  [name]
  (and (string? name)
       (>= (count name) 3)
       (<= (count name) 50)))

;; === Session Helpers ===

(defn logged-in?
  "Check if user is logged in.
   
   Examples:
   (logged-in? {:user {:username \"alice\"}}) => true
   (logged-in? {}) => false"
  [session]
  (boolean (:user session)))

(defn current-user
  "Get current user from session.
   
   Examples:
   (current-user {:user {:username \"alice\"}}) => {:username \"alice\"}
   (current-user {}) => nil"
  [session]
  (:user session))

(defn username
  "Get username from session.
   
   Examples:
   (username {:user {:username \"alice\"}}) => \"alice\"
   (username {}) => nil"
  [session]
  (get-in session [:user :username]))

;; === URL Helpers ===

(defn query-params
  "Build query string from map.
   
   Examples:
   (query-params {:page 2 :sort \"name\"}) => \"page=2&sort=name\"
   (query-params {}) => \"\""
  [params]
  (if (empty? params)
    ""
    (->> params
         (map (fn [[k v]] (str (name k) "=" (java.net.URLEncoder/encode (str v) "UTF-8"))))
         (clojure.string/join "&"))))

(defn url-with-params
  "Build URL with query parameters.
   
   Examples:
   (url-with-params \"/search\" {:q \"test\" :page 2})
   => \"/search?q=test&page=2\""
  [path params]
  (let [qs (query-params params)]
    (if (empty? qs)
      path
      (str path "?" qs))))

;; === JavaScript Helpers ===

(defn js-safe-string
  "Escape string for safe JavaScript inclusion.
   
   Examples:
   (js-safe-string \"Hello\") => \"Hello\"
   (js-safe-string \"It's\") => \"It\\\\'s\""
  [s]
  (-> s
      (clojure.string/replace "\\" "\\\\")
      (clojure.string/replace "'" "\\'")
      (clojure.string/replace "\"" "\\\"")
      (clojure.string/replace "\n" "\\n")))

(defn js-object
  "Convert Clojure map to JavaScript object literal.
   
   Examples:
   (js-object {:name \"Alice\" :age 30})
   => \"{name: 'Alice', age: 30}\""
  [m]
  (str "{"
       (->> m
            (map (fn [[k v]]
                   (str (name k) ": "
                       (cond
                         (string? v) (str "'" (js-safe-string v) "'")
                         (number? v) v
                         (boolean? v) v
                         (nil? v) "null"
                         :else (str "'" v "'")))))
            (clojure.string/join ", "))
       "}"))
