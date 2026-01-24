(ns cc.mindward.web-server.core
  (:require [cc.mindward.auth.interface :as auth]
            [cc.mindward.game.interface :as game]
            [cc.mindward.web-server.security :as security]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as res]
            [cc.mindward.user.interface :as user]
            [cc.mindward.ui.interface :as ui])
  (:gen-class))

;; ------------------------------------------------------------
;; μ (Directness): DRY Helpers
;; ------------------------------------------------------------

(defn- redirect-with-error
  "Redirect to path with URL-encoded error message.
   μ (Directness): Eliminates duplicate URL encoding logic."
  [path error-msg]
  (res/redirect (str path "?error=" (java.net.URLEncoder/encode error-msg "UTF-8"))))

(defn- redirect-with-success
  "Redirect to path with flash message, preserving session data.
   Optionally sets :username in session for auth flows."
  [session path success-msg & [username]]
  (-> (res/redirect path)
      (assoc :session (cond-> (or session {})
                        username (assoc :username username)
                        :always (assoc :flash {:type :success :message success-msg})))))

;; ------------------------------------------------------------
;; Input Validation (∀ Vigilance)
;; ------------------------------------------------------------

(def ^:private max-coordinate-array-size
  "Maximum number of coordinates allowed in a single request.
   Prevents memory exhaustion attacks."
  1000)

(defn- parse-coordinates
  "Parse coordinate JSON into a set of [x y] vectors with bounds validation.

   Security (∀ Vigilance):
   - Validates coordinates are within game board bounds
   - Limits array size to prevent memory exhaustion
   - Ensures coordinates are integers

   Returns: Set of [x y] vectors, empty set on error."
  [json-str]
  (try
    (let [coords (json/read-str (or json-str "[]"))
          ;; Validate it's a vector
          _ (when-not (vector? coords) (throw (Exception. "Not a vector")))
          ;; Limit array size to prevent memory exhaustion
          _ (when (> (count coords) max-coordinate-array-size)
              (throw (Exception. "Too many coordinates")))
          ;; Parse and validate each coordinate
          validated (into []
                          (comp (map (fn [c]
                                       (when (and (vector? c) (= 2 (count c)))
                                         (let [x (int (first c))
                                               y (int (second c))]
                                         ;; Bounds check using game config
                                           (when (and (<= (game/board-min-x) x (game/board-max-x))
                                                      (<= (game/board-min-y) y (game/board-max-y)))
                                             [x y])))))
                                (filter some?))
                          coords)]
      (set validated))
    (catch Exception e
      (log/warn "Coordinate parsing failed:" (.getMessage e))
      #{})))

(defn- validate-game-name
  "Validate game name for save/load operations.
   
   Rules:
   - Length: 3-100 characters
   - Safe characters only (no HTML/script injection)
   
   Returns: {:valid? boolean :error string}"
  [game-name]
  (cond
    (nil? game-name)
    {:valid? false :error "Game name is required"}

    (not (string? game-name))
    {:valid? false :error "Game name must be a string"}

    (< (count game-name) 3)
    {:valid? false :error "Game name must be at least 3 characters"}

    (> (count game-name) 100)
    {:valid? false :error "Game name must be at most 100 characters"}

    (not (re-matches #"^[a-zA-Z0-9_\-\s]+$" game-name))
    {:valid? false :error "Game name contains invalid characters"}

    :else
    {:valid? true}))

(defn leaderboard-page [{:keys [session]}]
  (let [leaderboard (user/get-leaderboard)]
    (ui/leaderboard-page session leaderboard)))

(defn signup-page [request]
  (let [{:keys [session params anti-forgery-token]} request]
    (if (:username session)
      (res/redirect "/select-game")
      (ui/signup-page session params anti-forgery-token))))

(defn handle-signup [{:keys [params session]}]
  (let [username (:username params)
        password (:password params)
        display-name (:name params)
        ;; Validate inputs (∀ Vigilance - validate at boundaries)
        username-check (security/validate-username username)
        password-check (security/validate-password password)
        name-check (security/validate-name display-name)]
    (cond
      (not (:valid? username-check))
      (do
        (log/warn "Invalid username on signup:" (:error username-check))
        (redirect-with-error "/signup" (:error username-check)))

      (not (:valid? password-check))
      (do
        (log/warn "Invalid password on signup:" (:error password-check))
        (redirect-with-error "/signup" (:error password-check)))

      (not (:valid? name-check))
      (do
        (log/warn "Invalid display name on signup:" (:error name-check))
        (redirect-with-error "/signup" (:error name-check)))

      :else
      (try
        (user/create-user! params)
        (log/info "User created successfully:" username)
        (redirect-with-success session "/select-game" (str "Identity established. Welcome, " username ".") username)
        (catch Exception e
          (log/warn e "User creation failed:" username)
          (redirect-with-error "/signup" "Username already exists"))))))

(defn shooter-page [request]
  (let [{:keys [session anti-forgery-token]} request]
    (if-let [username (:username session)]
      (let [high-score (user/get-high-score username)]
        (ui/shooter-page session anti-forgery-token high-score))
      (res/redirect "/login"))))

(defn game-life-page [request]
  (let [{:keys [session]} request]
    (if (:username session)
      (ui/game-life-page session)
      (res/redirect "/login"))))

(defn select-game-page [request]
  (let [{:keys [session]} request]
    (if (:username session)
      (ui/select-game-page session)
      (res/redirect "/login"))))

(defn landing-page [{:keys [session]}]
  (if (:username session)
    (res/redirect "/select-game")
    (ui/landing-page session)))

(defn login-page [{:keys [session params anti-forgery-token]}]
  (if (and (:username session) (not= (:force params) "true"))
    (res/redirect "/select-game")
    (ui/login-page session params anti-forgery-token)))

(defn handle-login [{:keys [params session]}]
  (let [username (:username params)
        password (:password params)
        ;; Validate inputs (∀ Vigilance)
        username-check (security/validate-username username)]
    (if (not (:valid? username-check))
      (do
        (log/warn "Invalid username on login:" (:error username-check))
        (redirect-with-error "/login" "Invalid credentials"))
      (let [auth-result (auth/authenticate username password)]
        (if auth-result
          (do
            (log/info "User logged in successfully:" username)
            (redirect-with-success session "/select-game" (str "Access granted. Connection established, " username ".") username))
          (do
            (log/warn "Failed login attempt for user:" username)
            (redirect-with-error "/login" "Invalid credentials")))))))

(defn save-score [{:keys [params session]}]
  (when-let [username (:username session)]
    (let [score-validation (security/validate-score (:score params "0"))
          score (:value score-validation)]
      (if (:valid? score-validation)
        (do
          (user/update-high-score! username score)
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (str "{\"highScore\": " (user/get-high-score username) "}")})
        (do
          (log/warn "Invalid score submission:" (:error score-validation) "from user:" username)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (str "{\"error\": \"" (:error score-validation) "\"}")})))))

(defn handle-logout [_]
  (-> (res/redirect "/login")
      (assoc :session nil)))

(defn- require-authentication
  "Require user to be authenticated. Returns 401 if not logged in.
   
   Security (∀ Vigilance): Authorization check at API boundary."
  [session]
  (if-let [username (:username session)]
    username
    (do
      (log/warn "Unauthorized API access attempt")
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "Authentication required"})})))

(defn game-api [{:keys [session params]}]
  ;; First, ensure user is authenticated
  (let [username (require-authentication session)]
    (if (string? username) ;; Check if we got a username or an error response
      (let [game-id (keyword (str "user-" username "-game"))
            action (:action params)]
        (case action
          "create"
          (let [cells (parse-coordinates (:cells params))]
            (game/create-game! game-id cells)
            (res/response (json/write-str {:board (into [] (game/get-board game-id))
                                           :generation (game/get-generation game-id)
                                           :score (game/get-score game-id)})))

          "evolve"
          (let [evolved (game/evolve! game-id)]
            (res/response (json/write-str {:board (into [] evolved)
                                           :generation (game/get-generation game-id)
                                           :score (game/get-score game-id)
                                           :triggers (game/get-musical-triggers game-id)})))

          "manipulate"
          (let [cells-to-add (parse-coordinates (:cells params))
                cells-to-remove (parse-coordinates (:remove params))]
            (game/add-cells! game-id cells-to-add)
            (game/clear-cells! game-id cells-to-remove)
            (res/response (json/write-str {:board (into [] (game/get-board game-id))
                                           :generation (game/get-generation game-id)
                                           :score (game/get-score game-id)})))

          "save"
          (let [game-name (:name params)
                name-validation (validate-game-name game-name)]
            (if (:valid? name-validation)
              (let [board (game/get-board game-id)
                    generation (game/get-generation game-id)
                    score (game/get-score game-id)
                    saved (user/save-game! username game-name board generation score)]
                (res/response (json/write-str {:id (:id saved)
                                               :name (:name saved)
                                               :saved true})))
              (res/bad-request (json/write-str {:error (:error name-validation)}))))

          "load"
          (if-let [saved-id (:savedId params)]
            (if-let [loaded (user/load-game! saved-id)]
              (let [board-set (set (map vec (:board loaded)))]
                (game/create-game! game-id board-set)
                (res/response (json/write-str {:board (:board loaded)
                                               :generation 0
                                               :score (game/get-score game-id)
                                               :loaded true})))
              (res/not-found (json/write-str {:error "Game not found"})))
            (res/bad-request (json/write-str {:error "Saved ID required"})))

          "delete"
          (if-let [saved-id (:savedId params)]
            (do
              (user/delete-game! saved-id)
              (res/response (json/write-str {:deleted true})))
            (res/bad-request (json/write-str {:error "Saved ID required"})))

          (res/bad-request (json/write-str {:error "Invalid action"}))))
      username))) ;; Return 401 response

(defn list-saved-games-api [{:keys [session]}]
  (let [username (require-authentication session)]
    (if (string? username)
      (res/response (json/write-str (user/list-saved-games username)))
      username)))

;; ------------------------------------------------------------
;; Logging Middleware
;; ------------------------------------------------------------

(defn wrap-request-logging
  "Middleware to log incoming requests and responses.
   Logs method, URI, status, and response time."
  [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          {:keys [request-method uri]} request
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)
          status (:status response)]
      (if (= uri "/health")
        ;; Don't log health check requests (too noisy)
        response
        (do
          (log/info (format "%s %s -> %s (%dms)"
                            (str/upper-case (name request-method))
                            uri
                            status
                            duration))
          response)))))

(defn wrap-error-logging
  "Middleware to catch and log uncaught exceptions."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Uncaught exception in request handler:"
                   {:method (:request-method request)
                    :uri (:uri request)
                    :params (:params request)})
        {:status 500
         :headers {"Content-Type" "text/html"}
         :body "<html><body><h1>500 Internal Server Error</h1><p>An error occurred. Please try again.</p></body></html>"}))))

;; ------------------------------------------------------------
;; Health Check Endpoint (Production Monitoring)
;; ------------------------------------------------------------

(defn health-check
  "Health check endpoint for load balancers and monitoring.
   Verifies database connectivity and game engine status."
  [_]
  (try
    (let [start-time (System/currentTimeMillis)
          ;; Test database connectivity by running a simple query
          db-healthy? (try
                        (some? (user/get-leaderboard))
                        (catch Exception e
                          (log/error e "Database health check failed")
                          false))
          ;; Check game engine health
          game-health (game/health-check)
          game-healthy? (:healthy? game-health)
          response-time (- (System/currentTimeMillis) start-time)
          overall-healthy? (and db-healthy? game-healthy?)
          status (if overall-healthy? "healthy" "unhealthy")]
      {:status (if overall-healthy? 200 503)
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status status
                              :timestamp (System/currentTimeMillis)
                              :checks {:database {:status (if db-healthy? "up" "down")}
                                       :game (:details game-health)
                                       :responseTimeMs response-time}
                              :version "1.0.0"})})
    (catch Exception e
      (log/error e "Health check endpoint failed")
      {:status 503
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status "unhealthy"
                              :error "Internal server error"})})))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get landing-page}]
     ["/health" {:get health-check}]
     ["/login" {:get login-page
                :post handle-login}]
     ["/signup" {:get signup-page
                 :post handle-signup}]
     ["/logout" {:get handle-logout}]
     ["/leaderboard" {:get leaderboard-page}]
     ["/select-game" {:get select-game-page}]
     ["/game/shooter" {:get shooter-page}]
     ["/game/life" {:get game-life-page}]
     ["/game/score" {:post save-score}]
     ["/api/game" {:post game-api}]
     ["/api/games" {:get list-saved-games-api}]
     ["/api/leaderboard" {:get (fn [_] {:status 200
                                        :headers {"Content-Type" "application/json"}
                                        :body (json/write-str (user/get-leaderboard))})}]])
   (ring/create-default-handler)))

(def site-app
  (-> app
      wrap-error-logging
      wrap-request-logging
      (wrap-defaults site-defaults)
      ;; Rate limiting on auth endpoints (prevents brute force)
      (security/wrap-rate-limit {:paths #{"/login" "/signup"}
                                 :max-requests 10
                                 :refill-rate 0.5}) ;; 1 request per 2 seconds
      ;; Rate limiting on game API (prevents spam/DoS)
      (security/wrap-rate-limit {:paths #{"/api/game" "/game/score"}
                                 :max-requests 30
                                 :refill-rate 2.0}) ;; 15 requests per second
      ;; Security headers (CSP, X-Frame-Options, etc.) - must be AFTER wrap-defaults
      security/wrap-security-headers))

(defn -main [& _args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Initializing database...")
    (user/init!)
    (log/info "Initializing game engine...")
    (game/initialize!)
    (log/info "Starting Cyberpunk Game Server on port" port "...")
    (jetty/run-jetty site-app {:port port :join? false})))
