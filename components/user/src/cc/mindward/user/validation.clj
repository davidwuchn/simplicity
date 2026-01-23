(ns cc.mindward.user.validation
  "Input validation configuration and helpers.
   
   (∃ Truth): Single source of truth for all validation rules.
   (fractal Clarity): Centralized configuration eliminates inconsistencies."
  (:require [clojure.string :as str]))

;; ------------------------------------------------------------
;; Validation Rules Configuration
;; (∃ Truth): All validation constants in one place
;; ------------------------------------------------------------

(def username-min-length
  "Minimum username length (characters)"
  3)

(def username-max-length
  "Maximum username length (characters)"
  32)

(def username-pattern
  "Valid username pattern: letters, numbers, dash, underscore"
  #"^[a-zA-Z0-9_-]+$")

(def password-min-length
  "Minimum password length (characters)"
  8)

(def name-min-length
  "Minimum display name length (characters)"
  3)

(def name-max-length
  "Maximum display name length (characters)"
  50)

(def score-max-value
  "Maximum allowed score value"
  1000000)

;; ------------------------------------------------------------
;; Validation Helpers
;; (∀ Vigilance): Validate at boundaries
;; ------------------------------------------------------------

(defn safe-parse-int
  "Safely parse integer with default value.
   
   Returns: Integer value or default-val if parsing fails.
   
   Use this instead of Integer/parseInt to prevent exceptions."
  [s default-val]
  (try
    (Integer/parseInt s)
    (catch Exception _
      default-val)))

(defn validate-username
  "Validate username format.
   
   Rules:
   - Length: 3-32 characters
   - Characters: alphanumeric, dash, underscore only
   - No leading/trailing whitespace
   
   Returns: {:valid? boolean :error string}"
  [username]
  (cond
    (nil? username)
    {:valid? false :error "Username is required"}
    
    (not (string? username))
    {:valid? false :error "Username must be a string"}
    
    (not= username (str/trim username))
    {:valid? false :error "Username cannot have leading/trailing whitespace"}
    
    (< (count username) username-min-length)
    {:valid? false :error (str "Username must be at least " username-min-length " characters")}
    
    (> (count username) username-max-length)
    {:valid? false :error (str "Username must be at most " username-max-length " characters")}
    
    (not (re-matches username-pattern username))
    {:valid? false :error "Username can only contain letters, numbers, dash, and underscore"}
    
    :else
    {:valid? true}))

(defn validate-password
  "Validate password strength.
   
   Rules:
   - Length: minimum 8 characters
   - No maximum (bcrypt handles long passwords)
   
   Returns: {:valid? boolean :error string}"
  [password]
  (cond
    (nil? password)
    {:valid? false :error "Password is required"}
    
    (not (string? password))
    {:valid? false :error "Password must be a string"}
    
    (< (count password) password-min-length)
    {:valid? false :error (str "Password must be at least " password-min-length " characters")}
    
    :else
    {:valid? true}))

(defn validate-name
  "Validate display name.
   
   Rules:
   - Length: 3-50 characters
   
   Returns: {:valid? boolean :error string}"
  [name]
  (cond
    (nil? name)
    {:valid? false :error "Name is required"}
    
    (not (string? name))
    {:valid? false :error "Name must be a string"}
    
    (< (count name) name-min-length)
    {:valid? false :error (str "Name must be at least " name-min-length " characters")}
    
    (> (count name) name-max-length)
    {:valid? false :error (str "Name must be at most " name-max-length " characters")}
    
    :else
    {:valid? true}))

(defn validate-score
  "Validate score value.
   
   Rules:
   - Must be a non-negative integer
   - Maximum: 1,000,000 (reasonable game score limit)
   
   Returns: {:valid? boolean :error string :value int}"
  [score-str]
  (let [score (safe-parse-int score-str -1)]
    (cond
      (< score 0)
      {:valid? false :error "Score must be a non-negative integer" :value 0}
      
      (> score score-max-value)
      {:valid? false :error "Score exceeds maximum allowed value" :value score-max-value}
      
      :else
      {:valid? true :value score})))

;; ------------------------------------------------------------
;; Simple Boolean Validators (for UI helpers)
;; (μ Directness): Simpler API for client-side checks
;; ------------------------------------------------------------

(defn valid-username?
  "Check if username is valid.
   Returns true/false (simpler than validate-username)."
  [username]
  (:valid? (validate-username username)))

(defn valid-password?
  "Check if password is valid.
   Returns true/false (simpler than validate-password)."
  [password]
  (:valid? (validate-password password)))

(defn valid-name?
  "Check if display name is valid.
   Returns true/false (simpler than validate-name)."
  [name]
  (:valid? (validate-name name)))
