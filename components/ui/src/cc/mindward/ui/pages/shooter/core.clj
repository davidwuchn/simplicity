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
               [:script {:src (:tailwind cdn-links)}]
               [:link {:href (:font cdn-links) :rel "stylesheet"}]
               [:style "
              body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #050505; touch-action: none; }
              #gameCanvas { display: block; width: 100%; height: 100%; }

              /* HUD Styles */
              .hud-element {
                font-family: 'Orbitron', monospace;
                background: rgba(0, 0, 0, 0.7);
                backdrop-filter: blur(4px);
                border: 1px solid #00f0ff;
                padding: 8px 12px;
                font-size: 14px;
              }

              /* Tutorial Overlay */
              #tutorial-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.9);
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 9999;
                animation: fadeIn 0.3s ease-out;
              }
              #tutorial-overlay.hidden {
                display: none !important;
              }
              .tutorial-card {
                background: #000;
                border: 2px solid #fcee0a;
                box-shadow: 6px 6px 0px 0px #00f0ff;
                padding: 2rem;
                max-width: 500px;
                margin: 20px;
                animation: slideIn 0.3s ease-out;
              }
              @keyframes fadeIn {
                from { opacity: 0; }
                to { opacity: 1; }
              }
              @keyframes slideIn {
                from { transform: translateY(-50px); opacity: 0; }
                to { transform: translateY(0); opacity: 1; }
              }

              /* Mobile Controls */
              @media (max-width: 768px) {
                .hud-element { font-size: 12px; padding: 6px 10px; }
                #controls-hint { font-size: 10px; }
              }

              /* Touch Controls (future enhancement) */
              .touch-control {
                position: fixed;
                bottom: 20px;
                width: 60px;
                height: 60px;
                background: rgba(0, 240, 255, 0.2);
                border: 2px solid #00f0ff;
                border-radius: 50%;
                display: none;
              }
              @media (max-width: 768px) and (pointer: coarse) {
                .touch-control { display: block; }
              }
            "]]]
             [:body
              [:input {:type "hidden" :id "csrf-token" :value anti-forgery-token}]

               ;; Canvas (Full Screen)
              [:canvas {:id "gameCanvas" :class "cursor-none" :aria-label "Game canvas"}]

               ;; High Score (Upper Right)
              [:div {:class "absolute top-4 right-4 pointer-events-none z-10"}
               [:div {:class "hud-element text-cyan-400"}
                "BEST: " [:span {:id "high-score" :class "text-white font-bold"} (or high-score 0)]]]

               ;; Quit Button (Upper Left)
              [:div {:class "absolute top-4 left-4 z-10"}
               [:a {:href "/game/life"
                    :class "cyber-btn-secondary text-sm"
                    :aria-label "Play Conway's Life"}
                "← LIFE"]]

               ;; Controls Hint
              [:div {:id "controls-hint" :class "absolute bottom-4 right-4 text-gray-500 text-xs font-mono pointer-events-none text-right hud-element"}
               [:div "ARROWS to Move"]
               [:div "SPACE to Switch Weapon"]
               [:div "SHIFT for Shield"]
               [:div "R to Reboot"]
               [:div {:class "mt-2"} [:a {:href "/game/life" :class "text-red-500 hover:text-white pointer-events-auto"} "ABORT MISSION"]]
               [:div {:class "mt-2 text-gray-600"} "v" app-version]]

               ;; Game Over Overlay (hidden by default)
              [:div {:id "game-over-overlay"
                     :class "hidden fixed top-0 left-0 w-full h-full bg-black/90 flex items-center justify-center z-50"}
               [:div {:class "tutorial-card text-center"}
                [:h2 {:class "text-4xl font-black text-cyber-red mb-4 uppercase"} "Mission Failed"]
                [:div {:class "text-2xl text-cyber-cyan mb-6"}
                 "FINAL SCORE: " [:span {:id "final-score" :class "text-white"} "0"]]
                [:button {:class "cyber-btn w-full mb-2" :onclick "location.reload()"} "Retry Mission"]
                [:a {:href "/leaderboard" :class "cyber-btn-secondary w-full inline-block text-center"} "View Rankings"]]]

               ;; Game initialization script
              [:script (h/raw "
              function showGameOver(score) {
                document.getElementById('final-score').textContent = score;
                const gameOverOverlay = document.getElementById('game-over-overlay');
                if (gameOverOverlay) {
                  gameOverOverlay.classList.remove('hidden');
                }
              }
             ")]]

               ;; Current user info for leaderboard highlighting
             [:script (h/raw (str "const CURRENT_USERNAME = " (pr-str username) ";"))]

               ;; Load game modules in order
             [:script {:src (str "/js/game-config.js?v=" app-version)}]
             [:script {:src (str "/js/music-config.js?v=" app-version)}]
             [:script {:src (str "/js/audio-utils.js?v=" app-version)}]
             [:script {:src (str "/js/game.js?v=" app-version)}]))}))