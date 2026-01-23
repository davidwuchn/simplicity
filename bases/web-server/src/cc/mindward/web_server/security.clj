(ns cc.mindward.web-server.security
  "Security middleware for production hardening.
   
   Implements:
   - Security headers (CSP, X-Frame-Options, HSTS, etc.)
   - Rate limiting for authentication endpoints
   - Input validation helpers
   
   Philosophy (∀ Vigilance): Defense in depth. Layer security controls."
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]))

;; ------------------------------------------------------------
;; Security Headers Middleware (OWASP Best Practices)
;; ------------------------------------------------------------

(defn wrap-security-headers
  "Add security headers to all responses.
   
   Headers:
   - Content-Security-Policy: Prevents XSS and code injection
   - X-Frame-Options: Prevents clickjacking
   - X-Content-Type-Options: Prevents MIME sniffing
   - X-XSS-Protection: Browser XSS filter (legacy but harmless)
   - Strict-Transport-Security: Force HTTPS (only if enabled)
   - Referrer-Policy: Control referrer information leakage
   - Permissions-Policy: Restrict browser features
   
   Configuration:
   - ENABLE_HSTS=true: Enable Strict-Transport-Security header"
  [handler]
  (fn [request]
    (let [response (handler request)
          enable-hsts? (= "true" (System/getenv "ENABLE_HSTS"))
          base-headers {"Content-Security-Policy" 
                        (str "default-src 'self'; "
                             "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; "  ;; inline scripts + Tailwind CDN
                             "style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://fonts.googleapis.com; "   ;; inline styles + CDN
                             "img-src 'self' data:; "
                             "font-src 'self' https://fonts.gstatic.com; "  ;; Google Fonts
                             "connect-src 'self'; "
                             "frame-ancestors 'none';")
                        "X-Frame-Options" "DENY"
                        "X-Content-Type-Options" "nosniff"
                        "X-XSS-Protection" "1; mode=block"
                        "Referrer-Policy" "strict-origin-when-cross-origin"
                        "Permissions-Policy" "geolocation=(), microphone=(), camera=()"}
          hsts-header (when enable-hsts?
                       {"Strict-Transport-Security" "max-age=31536000; includeSubDomains"})
          security-headers (merge base-headers hsts-header)]
      (update response :headers merge security-headers))))

;; ------------------------------------------------------------
;; Rate Limiting (Token Bucket Algorithm)
;; ------------------------------------------------------------

(defonce ^{:doc "In-memory rate limit storage. Maps IP -> {:tokens count :last-refill timestamp}.
                Note (τ Wisdom): For production with multiple servers, use Redis.
                For single-server deployments, in-memory is sufficient."}
  rate-limit-store
  (atom {}))

(defn refill-tokens
  "Refill tokens based on time elapsed since last refill.
   
   Algorithm: Token bucket with constant refill rate.
   - Refill rate: 1 token per second
   - Max tokens: configurable burst capacity"
  [bucket-state now max-tokens refill-rate]
  (let [last-refill (:last-refill bucket-state 0)
        elapsed-seconds (/ (- now last-refill) 1000.0)
        new-tokens (min max-tokens
                       (+ (:tokens bucket-state max-tokens)
                          (* elapsed-seconds refill-rate)))]
    {:tokens new-tokens
     :last-refill now}))

(defn check-rate-limit
  "Check if request is within rate limit.
   
   Returns: {:allowed? boolean :remaining int}
   
   Parameters:
   - identifier: IP address or user ID
   - max-tokens: Maximum burst capacity
   - refill-rate: Tokens added per second
   - cost: Number of tokens consumed by this request"
  [identifier max-tokens refill-rate cost]
  (let [now (System/currentTimeMillis)
        result (atom nil)]
    (swap! rate-limit-store
           (fn [store]
             (let [bucket (get store identifier {:tokens max-tokens :last-refill now})
                   refilled (refill-tokens bucket now max-tokens refill-rate)
                   new-tokens (:tokens refilled)
                   allowed? (>= new-tokens cost)
                   updated-tokens (if allowed? (- new-tokens cost) new-tokens)]
               (reset! result {:allowed? allowed? :remaining (int updated-tokens)})
               (assoc store identifier
                      {:tokens updated-tokens
                       :last-refill now}))))
    @result))

(defn get-client-ip
  "Extract client IP from request headers (supports proxies).
   
   Checks in order:
   1. X-Forwarded-For (most common proxy header)
   2. X-Real-IP (nginx)
   3. :remote-addr (direct connection)"
  [request]
  (or (first (str/split (get-in request [:headers "x-forwarded-for"] "") #","))
      (get-in request [:headers "x-real-ip"])
      (:remote-addr request)
      "unknown"))

(defn wrap-rate-limit
  "Rate limiting middleware for sensitive endpoints.
   
   Configuration:
   - paths: Set of paths to rate limit (e.g. /login, /signup)
   - max-requests: Maximum burst capacity (default 10)
   - refill-rate: Requests per second refill rate (default 0.5 = 1 per 2 seconds)
   - cost: Tokens consumed per request (default 1)
   
   Returns 429 Too Many Requests if limit exceeded."
  [handler {:keys [paths max-requests refill-rate cost]
            :or {max-requests 10
                 refill-rate 0.5
                 cost 1}}]
  (fn [request]
    (if (contains? paths (:uri request))
      (let [client-ip (get-client-ip request)
            rate-check (check-rate-limit client-ip max-requests refill-rate cost)]
        (if (:allowed? rate-check)
          (handler request)
          (do
            (log/warn "Rate limit exceeded for IP:" client-ip "on" (:uri request))
            {:status 429
             :headers {"Content-Type" "text/html"
                      "Retry-After" "60"}
             :body (str "<html><body><h1>429 Too Many Requests</h1>"
                       "<p>You have exceeded the rate limit. Please try again later.</p>"
                       "</body></html>")})))
      ;; Not a rate-limited path, proceed normally
      (handler request))))

;; ------------------------------------------------------------
;; Input Validation Helpers (∃ Truth - validate at boundaries)
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
    
    (not= username (clojure.string/trim username))
    {:valid? false :error "Username cannot have leading/trailing whitespace"}
    
    (< (count username) 3)
    {:valid? false :error "Username must be at least 3 characters"}
    
    (> (count username) 32)
    {:valid? false :error "Username must be at most 32 characters"}
    
    (not (re-matches #"^[a-zA-Z0-9_-]+$" username))
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
    
    (< (count password) 8)
    {:valid? false :error "Password must be at least 8 characters"}
    
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
      
      (> score 1000000)
      {:valid? false :error "Score exceeds maximum allowed value" :value 1000000}
      
      :else
      {:valid? true :value score})))
