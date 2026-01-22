(ns cc.mindward.ui.core-refactored
  "Refactored UI core following Clojure best practices.
   
   This namespace demonstrates:
   - Separation of concerns (styles, components, logic)
   - Pure functions
   - Data-driven design
   - DRY principles
   - Composable components"
  (:require [hiccup2.core :as h]
            [cc.mindward.ui.styles :as styles]
            [cc.mindward.ui.components :as c]
            [cc.mindward.ui.helpers :as helpers]))

;; === Configuration ===

(def ^:private meta-config
  {:charset "UTF-8"
   :viewport "width=device-width, initial-scale=1.0"
   :description "Musical Game of Life - Connect to the grid"})

(def ^:private cdn-links
  {:tailwind "https://cdn.tailwindcss.com"
   :font "https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&display=swap"})

;; === Scripts ===

(defn- toast-script []
  "JavaScript for toast notifications."
  "
  function showToast(message, type = 'info', duration = 3000) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type} fade-in`;
    toast.textContent = message;
    toast.setAttribute('role', 'status');
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.animation = 'slideIn 0.3s ease-out reverse';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }")

(defn- form-loading-script []
  "JavaScript for form loading states."
  "
  document.addEventListener('DOMContentLoaded', () => {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
      form.addEventListener('submit', (e) => {
        const btn = form.querySelector('button[type=submit]');
        if (btn) {
          btn.classList.add('loading');
          btn.disabled = true;
        }
      });
    });
  });")

;; === Head Component ===

(defn- page-head
  "Render the HTML head section.
   
   Options:
   - :title - Page title (required)"
  [{:keys [title]}]
  [:head
   [:meta {:charset (:charset meta-config)}]
   [:meta {:name "viewport" :content (:viewport meta-config)}]
   [:meta {:name "description" :content (:description meta-config)}]
   [:title title]
   [:script {:src (:tailwind cdn-links)}]
   [:link {:href (:font cdn-links) :rel "stylesheet"}]
   [:style (styles/stylesheet)]])

;; === Navigation Component ===

(defn- navigation
  "Render the main navigation.
   
   session - Ring session map"
  [session]
  (let [user (helpers/current-user session)]
    [:nav {:class "bg-black border-b-2 border-cyber-cyan p-4 mb-8"
           :role "navigation"
           :aria-label "Main navigation"}
     [:div {:class "container mx-auto flex justify-between items-center flex-wrap gap-4"}
      ;; Brand
      [:a {:href "/"
           :class "text-xl md:text-2xl font-black italic tracking-tighter text-cyber-yellow drop-shadow-[2px_2px_0px_#ff003c]"
           :aria-label "Home - Mindward Simplicity"}
       "MINDWARD // SIMPLICITY"]
      
      ;; Navigation Links
      [:div {:class "flex items-center space-x-4 md:space-x-6 uppercase tracking-widest text-xs md:text-sm"}
       (c/nav-link {:href "/leaderboard"
                    :text "Leaderboard"
                    :aria-label "View leaderboard"})
       
       (if user
         ;; Authenticated user menu
         [:div {:class "flex items-center space-x-2 md:space-x-4"}
          [:span {:class "text-gray-500 hidden-mobile"}
           "PILOT: "
           [:span {:class "font-bold text-cyber-cyan"} (:username user)]]
          (c/nav-link {:href "/game"
                       :text "JACK IN"
                       :aria-label "Start game"
                       :active? true})
          (c/nav-link {:href "/logout"
                       :text "ABORT"
                       :aria-label "Logout"})]
         
         ;; Guest menu
         [:div {:class "space-x-2 md:space-x-4"}
          (c/nav-link {:href "/login"
                       :text "LOGIN"
                       :aria-label "Login"})
          (c/link-button {:href "/signup"
                          :text "INITIATE"
                          :class "text-xs px-3 md:px-4 py-2"
                          :aria-label "Sign up"})])]]]))

;; === Layout Component ===

(defn layout
  "Main page layout.
   
   Options:
   - :session - Ring session map
   - :title - Page title (required)
   - :content - Page content (Hiccup) (required)
   - :extra-footer - Optional footer content (Hiccup)"
  [{:keys [session title content extra-footer]}]
  (str
   (h/html
    [:html {:lang "en"}
     (page-head {:title title})
     [:body
      ;; Toast container
      [:div {:id "toast-container" :role "alert" :aria-live "polite"}]
      
      ;; Navigation
      (navigation session)
      
      ;; Main content
      [:main {:class "container mx-auto px-4" :role "main"}
       content]
      
      ;; Footer/extra
      extra-footer
      
      ;; Scripts
      [:script (toast-script)]
      [:script (form-loading-script)]]])))

;; === Page Components ===

(defn leaderboard-page
  "Render the leaderboard page.
   
   Options:
   - :session - Ring session map
   - :leaderboard - Vector of leaderboard entries"
  [{:keys [session leaderboard]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout
          {:session session
           :title "Global Leaderboard"
           :content
           [:div {:class "max-w-4xl mx-auto fade-in"}
            [:h1 {:class "text-3xl md:text-5xl font-black text-cyber-yellow mb-8 md:mb-12 text-center uppercase tracking-widest glitch-text"}
             "Netrunner Legends"]
            
            (c/table
             {:columns ["Rank" "Netrunner" "Score"]
              :rows (map-indexed
                     (fn [idx {:keys [username name high_score]}]
                       {:cells [(cond
                                  (= idx 0) "👑 KING"
                                  (= idx 1) "🥈 02"
                                  (= idx 2) "🥉 03"
                                  :else (format "%02d" (inc idx)))
                                (or name username)
                                (helpers/format-number high_score)]
                        :options {:highlight? (zero? idx)}})
                     leaderboard)
              :empty-message "No data in the net."
              :corners? true})
            
            ;; Back to game button (if logged in)
            (when (helpers/logged-in? session)
              [:div {:class "mt-8 text-center"}
               (c/link-button {:href "/game"
                               :text "Return to Combat"
                               :type :secondary})])]})})

(defn signup-page
  "Render the signup page.
   
   Options:
   - :session - Ring session map
   - :params - Request params (may contain errors)
   - :anti-forgery-token - CSRF token"
   [{:keys [_session params anti-forgery-token]}]
   {:status 200
    :headers {"Content-Type" "text/html"}
    :body (layout
           {:session nil  ; Signup is for unauthenticated users
            :title "Join the Fleet"
            :content
            (c/card
             {:title "Identity Reg"
              :class "max-w-md mx-auto mt-8 md:mt-12 fade-in"
              :content
              [:<>
               ;; Error alert
               (when (:error params)
                 (c/alert {:type :error
                           :message "ERROR: IDENTITY CONFLICT DETECTED."}))
               
               ;; Signup form
               [:form {:method "POST"
                       :action "/signup"
                       :id "signup-form"
                       :aria-label "Sign up form"
                       :novalidate true}
                [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
                
                ;; Display Name
                (c/form-input {:id "name"
                               :name "name"
                               :label "Handle (Display)"
                               :placeholder "ENTER_HANDLE"
                               :required? true
                               :minlength 3
                               :maxlength 50
                               :autocomplete "name"
                               :help-text "3-50 characters"})
                
                ;; Username
                (c/form-input {:id "username"
                               :name "username"
                               :label "Net ID (Login)"
                               :placeholder "ENTER_ID"
                               :required? true
                               :minlength 3
                               :maxlength 20
                               :pattern "[a-zA-Z0-9_-]+"
                               :autocomplete "username"
                               :help-text "3-20 characters, alphanumeric only"})
                
                ;; Password
                (c/form-input {:id "password"
                               :name "password"
                               :label "Access Key (Password)"
                               :type "password"
                               :placeholder "********"
                               :required? true
                               :minlength 8
                               :autocomplete "new-password"
                               :help-text "Minimum 8 characters"})
                
                ;; Submit Button
                (c/button {:text "ESTABLISH LINK"
                           :type :primary
                           :submit? true
                           :aria-label "Create account"
                           :class "w-full"})
                
                ;; Login Link
                [:div {:class "mt-6 text-center text-xs md:text-sm"}
                 [:span {:class "text-gray-500"} "Already registered? "]
                 [:a {:href "/login"
                      :class "text-cyber-cyan hover:text-cyber-yellow transition-colors"}
                  "Login here"]]]
               
               ;; Client-side validation script
               [:script "
                 const form = document.getElementById('signup-form');
                 if (form) {
                   form.addEventListener('submit', (e) => {
                     const username = form.querySelector('[name=username]');
                     const password = form.querySelector('[name=password]');
                     const name = form.querySelector('[name=name]');
                     
                     if (username.value.length < 3 || username.value.length > 20) {
                       e.preventDefault();
                       showToast('Net ID must be 3-20 characters', 'error');
                       username.focus();
                       return;
                     }
                     if (!/^[a-zA-Z0-9_-]+$/.test(username.value)) {
                       e.preventDefault();
                       showToast('Net ID can only contain letters, numbers, _ and -', 'error');
                       username.focus();
                       return;
                     }
                     if (password.value.length < 8) {
                       e.preventDefault();
                       showToast('Access Key must be at least 8 characters', 'error');
                       password.focus();
                       return;
                     }
                     if (name.value.length < 3) {
                       e.preventDefault();
                       showToast('Handle must be at least 3 characters', 'error');
                       name.focus();
                       return;
                     }
                   });
                 }
               "]]})})})


(defn login-page
  "Render the login page.
   
   Options:
   - :session - Ring session map
   - :params - Request params (may contain errors)
   - :anti-forgery-token - CSRF token"
  [{:keys [session params anti-forgery-token]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout
          {:session session
           :title "Login"
           :content
           (c/card
            {:title "Net Access"
             :class "max-w-md mx-auto mt-8 md:mt-12 fade-in"
             :content
             [:<>
              ;; Error alert
              (when (:error params)
                (c/alert {:type :error
                          :message "ACCESS DENIED. INVALID CREDENTIALS."}))
              
              ;; Login form
              [:form {:method "POST"
                      :action "/login"
                      :id "login-form"
                      :aria-label "Login form"}
               [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
               
               ;; Username
               (c/form-input {:id "username-input"
                              :name "username"
                              :label "Net ID"
                              :placeholder "ID_REQUIRED"
                              :required? true
                              :autocomplete "username"})
               
               ;; Password
               (c/form-input {:id "password-input"
                              :name "password"
                              :label "Access Key"
                              :type "password"
                              :placeholder "KEY_REQUIRED"
                              :required? true
                              :autocomplete "current-password"})
               
               ;; Submit Button
               (c/button {:text "JACK IN"
                          :type :primary
                          :submit? true
                          :aria-label "Login"
                          :class "w-full"})
               
               ;; Signup Link
               [:div {:class "mt-6 text-center text-xs md:text-sm"}
                [:span {:class "text-gray-500"} "New to the grid? "]
                [:a {:href "/signup"
                     :class "text-cyber-cyan hover:text-cyber-yellow transition-colors"}
                 "Register here"]]]
              
              ;; Show toast on error
              (when (:error params)
                [:script "setTimeout(() => showToast('Invalid credentials. Try again.', 'error'), 100);"])]})})})

(defn landing-page
  "Render the landing page.
   
   Options:
   - :session - Ring session map"
  [{:keys [session]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout
          {:session session
           :title "Welcome"
           :content
           [:div
            ;; Background Canvas for Game of Life
            [:canvas {:id "bgCanvas"
                      :class "fixed top-0 left-0 w-full h-full z-0 pointer-events-auto"
                      :aria-hidden "true"}]
            [:input {:type "hidden" :id "csrf-token" :value (:csrf-token session "")}]

            [:div {:class "relative z-10 min-h-[80vh] flex flex-col justify-center items-center text-center pointer-events-none px-4"}
             [:div {:class "relative mb-8 md:mb-12 pointer-events-auto fade-in"}
              [:h1 {:class "text-4xl md:text-7xl font-black text-cyber-yellow mb-2 glitch-text uppercase tracking-tighter"}
               "MINDWARD"]
              [:div {:class "text-lg md:text-2xl font-bold text-cyber-cyan tracking-[0.5em] md:tracking-[1em] uppercase"}
               "Simplicity"]]

             [:p {:class "text-base md:text-xl text-gray-400 mb-12 md:mb-16 max-w-lg font-mono border-l-4 border-cyber-red pl-4 md:pl-6 text-left bg-black/50 p-3 md:p-4 pointer-events-auto backdrop-blur-sm fade-in"}
              "Connect to the grid. Engage hostile protocols. Ascend the hierarchy."]

             [:div {:class "flex flex-col sm:flex-row gap-4 sm:gap-8 pointer-events-auto w-full sm:w-auto fade-in"}
              (c/link-button {:href "/login"
                              :text "Login"
                              :type :secondary
                              :class "min-w-[150px]"
                              :aria-label "Login to existing account"})
              (c/link-button {:href "/signup"
                              :text "Initiate"
                              :type :primary
                              :class "min-w-[150px]"
                              :aria-label "Create new account"})]]]
           :extra-footer [:script {:src "/js/life.js"}]})})

(defn game-page
  "Render the game page (full-screen canvas game).
   
   Options:
   - :session - Ring session map (unused, for game page)
   - :anti-forgery-token - CSRF token
   - :high-score - User's high score"
  [_session anti-forgery-token high-score]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str
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
                display: none;
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
             [:div {:id "tutorial-overlay" :role "dialog" :aria-labelledby "tutorial-title"}
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
             
             ;; HUD Overlay (High Score)
             [:div {:class "absolute top-4 right-4 pointer-events-none z-10"}
              [:div {:class "hud-element text-cyan-400"}
               "BEST: " [:span {:id "high-score" :class "text-white font-bold"} high-score]]]
             
             ;; Current Score
             [:div {:class "absolute top-4 left-4 pointer-events-none z-10"}
              [:div {:class "hud-element text-cyber-yellow"}
               "SCORE: " [:span {:id "current-score" :class "text-white font-bold"} "0"]]]
             
             ;; Controls Hint
             [:div {:id "controls-hint" :class "absolute bottom-4 right-4 text-gray-500 text-xs font-mono pointer-events-none text-right hud-element"}
              [:div "ARROWS to Move"]
              [:div "SPACE to Switch Weapon"]
              [:div "R to Reboot"]
              [:div {:class "mt-2"} [:a {:href "/" :class "text-red-500 hover:text-white pointer-events-auto"} "ABORT MISSION"]]]
             
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
             [:script "
              let tutorialShown = localStorage.getItem('tutorial-shown') === 'true';
              
              function startGame() {
                document.getElementById('tutorial-overlay').classList.add('hidden');
                localStorage.setItem('tutorial-shown', 'true');
                initializeGame();
              }
              
              function skipTutorial() {
                startGame();
              }
              
              function initializeGame() {
                // Game will be initialized by game.js
                console.log('Game initialized');
              }
              
              // Show tutorial only on first visit
              if (!tutorialShown) {
                document.getElementById('tutorial-overlay').classList.remove('hidden');
              } else {
                document.getElementById('tutorial-overlay').classList.add('hidden');
                setTimeout(initializeGame, 100);
              }
              
              // Update current score (called from game.js)
              function updateScore(score) {
                document.getElementById('current-score').textContent = score;
              }
              
              // Show game over
              function showGameOver(score) {
                document.getElementById('final-score').textContent = score;
                document.getElementById('game-over-overlay').classList.remove('hidden');
              }
             "]
             
             [:script {:type "module" :src (str "/js/game.js?v=" (System/currentTimeMillis))}]]]))})
