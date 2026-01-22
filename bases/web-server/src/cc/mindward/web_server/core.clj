(ns cc.mindward.web-server.core
  (:require [cc.mindward.auth.interface :as auth]
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
    (user/create-user! params)
    (-> (res/redirect "/game")
        (assoc :session (assoc session :username (:username params))))
    (catch Exception _
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
  (let [auth-result (auth/authenticate (:username params) (:password params))]
    (if auth-result
      (-> (res/redirect "/game")
          (assoc :session (assoc session :username (:username params))))
      (res/redirect "/login?error=true"))))

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

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get landing-page}]
     ["/login" {:get login-page
                :post handle-login}]
     ["/signup" {:get signup-page
                 :post handle-signup}]
     ["/logout" {:get handle-logout}]
     ["/leaderboard" {:get leaderboard-page}]
     ["/game" {:get game-page}]
     ["/game/score" {:post save-score}]])
   (ring/create-default-handler)))

(def site-app
  (wrap-defaults app site-defaults))

(defn -main [& _args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Initializing database...")
    (user/init!)
    (log/info "Starting Cyberpunk Game Server on port" port "...")
    (jetty/run-jetty site-app {:port port :join? false})))
