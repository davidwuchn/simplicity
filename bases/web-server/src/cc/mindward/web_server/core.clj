(ns cc.mindward.web-server.core
  (:require [cc.mindward.auth.interface :as auth]
            [cc.mindward.game.interface :as game]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as res]
            [cc.mindward.user.interface :as user]
            [cc.mindward.ui.interface :as ui])
  (:gen-class))

(defn leaderboard-page [{:keys [session]}]
  (let [leaderboard (user/get-leaderboard)]
    (ui/leaderboard-page session leaderboard)))

(defn signup-page [request]
  (let [{:keys [session params anti-forgery-token]} request]
    (if (:username session)
      (res/redirect "/game")
      (ui/signup-page session params anti-forgery-token))))

(defn handle-signup [{:keys [params session]}]
  (try
    (let [username (:username params)]
      (user/create-user! params)
      (log/info "User created successfully:" username)
      (-> (res/redirect "/game")
          (assoc :session (assoc session :username username))))
    (catch Exception e
      (log/warn e "User creation failed:" (:username params))
      (res/redirect "/signup?error=true"))))

(defn game-page [request]
  (let [{:keys [session anti-forgery-token]} request]
    (if-let [username (:username session)]
      (let [high-score (user/get-high-score username)]
        (ui/game-page session anti-forgery-token high-score))
      (res/redirect "/login"))))

(defn landing-page [{:keys [session]}]
  (if (:username session)
    (res/redirect "/game")
    (ui/landing-page session)))

(defn login-page [{:keys [session params anti-forgery-token]}]
  (if (:username session)
    (res/redirect "/game")
    (ui/login-page session params anti-forgery-token)))

(defn handle-login [{:keys [params session]}]
  (let [username (:username params)
        auth-result (auth/authenticate username (:password params))]
    (if auth-result
      (do
        (log/info "User logged in successfully:" username)
        (-> (res/redirect "/game")
            (assoc :session (assoc session :username username))))
      (do
        (log/warn "Failed login attempt for user:" username)
        (res/redirect "/login?error=true")))))

(defn save-score [{:keys [params session]}]
  (when-let [username (:username session)]
    (let [score (Integer/parseInt (:score params "0"))]
      (user/update-high-score! username score)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (str "{\"highScore\": " (user/get-high-score username) "}")})))

(defn handle-logout [{:keys [_session]}]
  (-> (res/redirect "/login")
      (assoc :session nil)))

(defn game-api [{:keys [session params headers]}]
  (let [game-id (keyword (str "user-" (:username session "anonymous") "-game"))]
    (case (:action params)
      "create" (let [cells (into #{} (map (fn [[x y]] [(int x) (int y)])) 
                             (json/read-str (:cells params "[]")))]
                 (game/create-game! game-id cells)
                 (res/response (json/write-str {:board (into [] (game/get-board game-id))
                                                :generation (game/get-generation game-id)
                                                :score (game/get-score game-id)})))
      "evolve" (let [evolved (game/evolve! game-id)]
                 (res/response (json/write-str {:board (into [] evolved)
                                                :generation (game/get-generation game-id)
                                                :score (game/get-score game-id)
                                                :triggers (game/get-musical-triggers game-id)})))
      "manipulate" (let [cells-to-add (into #{} (map (fn [[x y]] [(int x) (int y)]))
                                         (json/read-str (:cells params "[]")))
                         cells-to-remove (into #{} (map (fn [[x y]] [(int x) (int y)]))
                                            (json/read-str (:remove params "[]")))]
                     (game/add-cells! game-id cells-to-add)
                     (game/clear-cells! game-id cells-to-remove)
                     (res/response (json/write-str {:board (into [] (game/get-board game-id))
                                                    :generation (game/get-generation game-id)
                                                    :score (game/get-score game-id)})))
      "save" (when-let [game-name (:name params)]
               (let [saved (game/save-game! game-id game-name)]
                 (res/response (json/write-str {:id (:id saved) 
                                                :name (:name saved)
                                                :saved true}))))
      "load" (when-let [saved-id (:savedId params)]
               (let [loaded (game/load-game! saved-id game-id)]
                 (res/response (json/write-str {:board (into [] loaded)
                                                :generation (game/get-generation game-id)
                                                :score (game/get-score game-id)
                                                :loaded true}))))
      (res/response (json/write-str {:error "Invalid action"})))))

(defn list-saved-games-api [_]
  (res/response (json/write-str (game/list-saved-games))))

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
                           (clojure.string/upper-case (name request-method))
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

(defn health-check [_]
  "Health check endpoint for load balancers and monitoring.
   Verifies database connectivity and returns system status."
  (try
    (let [start-time (System/currentTimeMillis)
          ;; Test database connectivity by running a simple query
          db-healthy? (try
                        (some? (user/get-leaderboard))
                        (catch Exception e
                          (log/error e "Database health check failed")
                          false))
          response-time (- (System/currentTimeMillis) start-time)
          status (if db-healthy? "healthy" "unhealthy")]
      {:status (if db-healthy? 200 503)
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status status
                              :timestamp (System/currentTimeMillis)
                              :checks {:database {:status (if db-healthy? "up" "down")
                                                 :responseTimeMs response-time}}
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
     ["/game" {:get game-page}]
     ["/game/score" {:post save-score}]
     ["/api/game" {:post game-api}]
     ["/api/games" {:get list-saved-games-api}]])
   (ring/create-default-handler)))

(def site-app
  (-> app
      wrap-error-logging
      wrap-request-logging
      (wrap-defaults site-defaults)))

(defn -main [& _args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Initializing database...")
    (user/init!)
    (log/info "Initializing game engine...")
    (game/initialize!)
    (log/info "Starting Cyberpunk Game Server on port" port "...")
    (jetty/run-jetty site-app {:port port :join? false})))
