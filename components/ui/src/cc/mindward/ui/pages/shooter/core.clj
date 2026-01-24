(ns cc.mindward.ui.pages.shooter.core
  "Space Shooter game page - full-screen canvas game with HUD overlay."
  (:require [hiccup2.core :as h]
            [cc.mindward.ui.layout :as layout]))

(defn shooter-page
  "Render the Space Shooter game page.

   Options:
   - :session - Ring session map (contains :username)
   - :anti-forgery-token - CSRF token
   - :high-score - User's high score"
  [session anti-forgery-token high-score]
  (let [cdn-links (layout/cdn-links-map)
        app-version (layout/app-version-string)
        username (:username session)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str
            "<!DOCTYPE html>\n"
            (h/html
             [:html {:lang "en"}
              [:head
               [:meta {:charset "UTF-8"}]
               [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
               [:title "Space Shooter - Simplicity"]
               [:script {:src "/js/tailwind.js"}]
               [:script {:src "/js/tailwind-config.js"}]
               [:link {:href (:font cdn-links) :rel "stylesheet"}]
               [:link {:href "/css/main.css" :rel "stylesheet"}]]
             [:body
              [:input {:type "hidden" :id "csrf-token" :value anti-forgery-token}]
              [:input {:type "hidden" :id "current-username" :value username}]

               ;; Canvas (Full Screen)
              [:canvas {:id "gameCanvas" :class "cursor-none" :aria-label "Game canvas"}]

               ;; High Score (Upper Right)
              [:div {:class "absolute top-4 right-4 pointer-events-none z-10"}
               [:div {:class "hud-element text-cyan-400"}
                "BEST: " [:span {:id "high-score" :class "text-white font-bold"} (or high-score 0)]]]

               ;; Quit Button (Upper Left)
              [:div {:class "absolute top-4 left-4 z-10"}
               [:a {:href "/select-game"
                    :class "cyber-btn-secondary text-sm"
                    :aria-label "Return to game selection"}
                "← QUIT"]]

               ;; Controls Hint
              [:div {:id "controls-hint" :class "absolute bottom-4 right-4 text-gray-500 text-xs font-mono pointer-events-none text-right hud-element"}
               [:div "ARROWS to Move"]
               [:div "SPACE to Switch Weapon"]
               [:div "SHIFT for Shield"]
               [:div "R to Reboot"]
               [:div {:class "mt-2"} [:a {:href "/select-game" :class "text-red-500 hover:text-white pointer-events-auto"} "ABORT MISSION"]]
               [:div {:class "mt-2 text-gray-600"} "v" app-version]]

               ;; Game Over Overlay (hidden by default)
              [:div {:id "game-over-overlay"
                     :class "hidden fixed top-0 left-0 w-full h-full bg-black/90 flex items-center justify-center z-50"}
               [:div {:class "tutorial-card text-center"}
                [:h2 {:class "text-4xl font-black text-cyber-red mb-4 uppercase"} "Mission Failed"]
                [:div {:class "text-2xl text-cyber-cyan mb-6"}
                 "FINAL SCORE: " [:span {:id "final-score" :class "text-white"} "0"]]
                [:button {:class "cyber-btn w-full mb-2" :id "retry-btn"} "Retry Mission"]
                [:a {:href "/leaderboard" :class "cyber-btn-secondary w-full inline-block text-center"} "View Rankings"]]]

               ;; Load game UI logic (must be before game.js)
             [:script {:src (str "/js/shooter-ui.js?v=" app-version)}]

               ;; Load game modules in order
             [:script {:src (str "/js/game-config.js?v=" app-version)}]
             [:script {:src (str "/js/music-config.js?v=" app-version)}]
             [:script {:src (str "/js/audio-utils.js?v=" app-version)}]
             [:script {:src (str "/js/game.js?v=" app-version)}]
             
             ;; Attach Retry Button listener (no inline onclick)
             [:script (h/raw "
               document.addEventListener('DOMContentLoaded', () => {
                 const retryBtn = document.getElementById('retry-btn');
                 if(retryBtn) retryBtn.addEventListener('click', () => location.reload());
               });
             ")]
             ]))}))