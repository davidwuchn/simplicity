(ns cc.mindward.ui.pages.game
  "Game page - full-screen canvas game with HUD overlay."
  (:require [hiccup2.core :as h]
            [cc.mindward.ui.layout :as layout]))

(defn game-page
  "Render the game page (full-screen canvas game).
   
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
            "]]
              [:body
               [:input {:type "hidden" :id "csrf-token" :value anti-forgery-token}]
               
               ;; Tutorial Overlay
               [:div {:id "tutorial-overlay" :role "dialog" :aria-labelledby "tutorial-title" :style "display: none;"}
                [:div {:class "tutorial-card text-center"}
                 [:h2 {:id "tutorial-title" :class "text-3xl font-black text-cyber-yellow mb-6 uppercase"} "Mission Briefing"]
                 [:div {:class "text-left space-y-4 text-gray-300 mb-6"}
                  [:div {:class "flex items-center gap-3"}
                   [:span {:class "text-cyber-cyan text-xl"} "⬆⬇⬅➡"]
                   [:span "Arrow Keys - Navigate your ship"]]
                  [:div {:class "flex items-center gap-3"}
                   [:span {:class "text-cyber-yellow text-xl"} "SPACE"]
                   [:span "Switch weapon systems"]]
                  [:div {:class "flex items-center gap-3"}
                   [:span {:class "text-cyber-red text-xl"} "R"]
                   [:span "Emergency reboot (restart)"]]
                  [:div {:class "flex items-center gap-3"}
                   [:span {:class "text-green-400 text-xl"} "🎯"]
                   [:span "Destroy enemies to level up"]]]
                 [:button {:id "start-game-btn" 
                           :class "cyber-btn w-full mt-4"
                           :onclick "startGame()"} 
                  "Launch Mission"]
                 [:button {:id "skip-tutorial-btn"
                           :class "cyber-btn-secondary w-full mt-2"
                           :onclick "skipTutorial()"}
                  "Skip Briefing"]]]
               
               ;; Canvas (Full Screen)
               [:canvas {:id "gameCanvas" :class "cursor-none" :aria-label "Game canvas"}]
               
                ;; High Score (Upper Right)
                 [:div {:class "absolute top-4 right-4 pointer-events-none z-10"}
                  [:div {:class "hud-element text-cyan-400"}
                   "BEST: " [:span {:id "high-score" :class "text-white font-bold"} (or high-score 0)]]]
                
               ;; Controls Hint
               [:div {:id "controls-hint" :class "absolute bottom-4 right-4 text-gray-500 text-xs font-mono pointer-events-none text-right hud-element"}
                [:div "ARROWS to Move"]
                [:div "SPACE to Switch Weapon"]
                [:div "SHIFT for Shield"]
                [:div "R to Reboot"]
                [:div {:class "mt-2"} [:a {:href "/" :class "text-red-500 hover:text-white pointer-events-auto"} "ABORT MISSION"]]
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
               
               ;; Tutorial and Game initialization script
               [:script (h/raw "
              let tutorialShown = localStorage.getItem('tutorial-shown') === 'true';
              
              function startGame() {
                const overlay = document.getElementById('tutorial-overlay');
                if (overlay) {
                  overlay.style.display = 'none';
                  localStorage.setItem('tutorial-shown', 'true');
                  initializeGame();
                }
              }
              
              function skipTutorial() {
                startGame();
              }
              
              function initializeGame() {
                // Game will be initialized by game.js
                console.log('Game initialized');
              }
              
              // Wait for DOM to be ready
              document.addEventListener('DOMContentLoaded', function() {
                const overlay = document.getElementById('tutorial-overlay');
                if (!overlay) return;
                
                // Show tutorial only on first visit
                if (!tutorialShown) {
                  overlay.style.display = 'flex';
                } else {
                  overlay.style.display = 'none';
                  setTimeout(initializeGame, 100);
                }
              });
              
              // Show game over
              function showGameOver(score) {
                document.getElementById('final-score').textContent = score;
                const gameOverOverlay = document.getElementById('game-over-overlay');
                if (gameOverOverlay) {
                  gameOverOverlay.classList.remove('hidden');
                }
              }
             ")]
               
               ;; Current user info for leaderboard highlighting
               [:script (h/raw (str "const CURRENT_USERNAME = " (pr-str username) ";"))]
               
               ;; Load game modules in order
               [:script {:src (str "/js/game-config.js?v=" app-version)}]
               [:script {:src (str "/js/music-config.js?v=" app-version)}]
               [:script {:src (str "/js/audio-utils.js?v=" app-version)}]
               [:script {:src (str "/js/game.js?v=" app-version)}]]]))})
)