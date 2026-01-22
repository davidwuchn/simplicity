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

(defn game-api [{:keys [session params headers]}]
  (let [game-id (keyword (str "user-" (:username session "anonymous") "-game"))]
    (case (:action params)
      "create" (do
                 (game/clear-cells! game-id (game/get-board game-id))
                 (game/add-cells! game-id (into #{} (map (fn [[x y]] [(int x) (int y)])) 
                                   (json/read-str (:cells params "[]"))))
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
                                            (json/read-str (:remove params "[]")))
                         new-board (-> game-id
                                      (game/add-cells! cells-to-add)
                                      (game/clear-cells! cells-to-remove))]
                     (res/response (json/write-str {:board (into [] new-board)
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
     ["/game/score" {:post save-score}]
     ["/api/game" {:post game-api}]
     ["/api/games" {:get list-saved-games-api}]])
   (ring/create-default-handler)))

(def site-app
  (wrap-defaults app site-defaults))

(defn -main [& _args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Initializing database...")
    (user/init!)
    (log/info "Initializing game engine...")
    (game/initialize!)
    (log/info "Starting Cyberpunk Game Server on port" port "...")
    (jetty/run-jetty site-app {:port port :join? false})))
