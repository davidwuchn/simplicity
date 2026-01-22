(ns cc.mindward.ui.core
  (:require [hiccup2.core :as h]))

(defn layout [session title content & [extra-footer]]
  (let [username (:username session)]
    (str
     (h/html
      [:html {:lang "en"}
       [:head
        [:meta {:charset "UTF-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
        [:meta {:name "description" :content "Musical Game of Life - Connect to the grid"}]
        [:title title]
        [:script {:src "https://cdn.tailwindcss.com"}]
        [:link {:href "https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&display=swap" :rel "stylesheet"}]
        [:style "
          /* === Base Styles === */
          * { box-sizing: border-box; }
          body { 
            background-color: #050505; 
            color: #e2e8f0; 
            font-family: 'Orbitron', sans-serif; 
            background-image: linear-gradient(0deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent); 
            background-size: 50px 50px;
            min-height: 100vh;
          }
          
          /* === Cyber Components === */
          .font-cyber { font-family: 'Orbitron', sans-serif; }
          .cyber-card { 
            background-color: #000; 
            border: 2px solid #fcee0a; 
            box-shadow: 6px 6px 0px 0px #00f0ff; 
            padding: 2rem;
            transition: all 0.3s ease;
          }
          .cyber-card:hover {
            box-shadow: 8px 8px 0px 0px #00f0ff, 0 0 20px rgba(0, 240, 255, 0.3);
            transform: translate(-2px, -2px);
          }
          
          .cyber-input { 
            background-color: #1a1a1a; 
            border: 1px solid #00f0ff; 
            color: #00f0ff; 
            border-radius: 0;
            transition: all 0.2s ease;
          }
          .cyber-input:focus { 
            border-color: #fcee0a; 
            outline: none; 
            box-shadow: 0 0 10px #fcee0a, inset 0 0 5px rgba(252, 238, 10, 0.1);
            transform: scale(1.01);
          }
          .cyber-input:invalid:not(:placeholder-shown) {
            border-color: #ff003c;
            box-shadow: 0 0 10px rgba(255, 0, 60, 0.5);
          }
          
          .cyber-btn { 
            background-color: #fcee0a; 
            color: #000; 
            font-weight: 900; 
            text-transform: uppercase; 
            border: none; 
            clip-path: polygon(10% 0, 100% 0, 100% 70%, 90% 100%, 0 100%, 0 30%); 
            padding: 12px 24px; 
            transition: all 0.2s ease;
            cursor: pointer;
            position: relative;
            overflow: hidden;
          }
          .cyber-btn::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 0;
            height: 0;
            background: rgba(255, 255, 255, 0.3);
            border-radius: 50%;
            transform: translate(-50%, -50%);
            transition: width 0.3s, height 0.3s;
          }
          .cyber-btn:hover::before {
            width: 300px;
            height: 300px;
          }
          .cyber-btn:hover { 
            background-color: #00f0ff; 
            box-shadow: 4px 4px 0px #ff003c; 
            transform: translate(-2px, -2px); 
          }
          .cyber-btn:active {
            transform: translate(0, 0);
            box-shadow: 2px 2px 0px #ff003c;
          }
          .cyber-btn:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            transform: none;
          }
          
          .cyber-btn-secondary { 
            background-color: #2a2a2a; 
            color: #00f0ff; 
            border: 2px solid #00f0ff; 
            font-weight: 700; 
            text-transform: uppercase; 
            clip-path: polygon(10% 0, 100% 0, 100% 70%, 90% 100%, 0 100%, 0 30%); 
            padding: 10px 22px; 
            transition: all 0.2s ease;
            cursor: pointer;
          }
          .cyber-btn-secondary:hover { 
            background-color: #00f0ff; 
            color: #000; 
            box-shadow: 4px 4px 0px #fcee0a;
            transform: translate(-2px, -2px);
          }
          
          /* === Animations === */
          .glitch-text { 
            text-shadow: 2px 0 #ff003c, -2px 0 #00f0ff; 
            animation: glitch 1s infinite alternate-reverse; 
          }
          @keyframes glitch { 
            0% { text-shadow: 2px 0 #ff003c, -2px 0 #00f0ff; } 
            25% { text-shadow: -2px 0 #ff003c, 2px 0 #00f0ff; } 
            50% { text-shadow: 2px 0 #00f0ff, -2px 0 #fcee0a; } 
            100% { text-shadow: -2px 0 #00f0ff, 2px 0 #ff003c; } 
          }
          
          @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
          }
          .fade-in {
            animation: fadeIn 0.5s ease-out;
          }
          
          @keyframes slideIn {
            from { transform: translateX(-100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
          }
          .slide-in {
            animation: slideIn 0.3s ease-out;
          }
          
          /* === Colors === */
          .text-cyber-yellow { color: #fcee0a; }
          .text-cyber-cyan { color: #00f0ff; }
          .text-cyber-red { color: #ff003c; }
          .border-cyber-yellow { border-color: #fcee0a; }
          .border-cyber-cyan { border-color: #00f0ff; }
          .border-cyber-red { border-color: #ff003c; }
          
          /* === Loading State === */
          .loading {
            position: relative;
            pointer-events: none;
            opacity: 0.6;
          }
          .loading::after {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 20px;
            height: 20px;
            margin: -10px 0 0 -10px;
            border: 2px solid #00f0ff;
            border-top-color: transparent;
            border-radius: 50%;
            animation: spin 0.6s linear infinite;
          }
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
          
          /* === Toast Notifications === */
          #toast-container {
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 9999;
            display: flex;
            flex-direction: column;
            gap: 10px;
            pointer-events: none;
          }
          .toast {
            background: #000;
            border: 2px solid;
            padding: 16px 20px;
            min-width: 300px;
            max-width: 400px;
            box-shadow: 6px 6px 0px 0px;
            animation: slideIn 0.3s ease-out;
            pointer-events: auto;
            font-size: 14px;
            font-weight: 700;
          }
          .toast.success { border-color: #00ff00; box-shadow: 6px 6px 0px 0px #00ff00; color: #00ff00; }
          .toast.error { border-color: #ff003c; box-shadow: 6px 6px 0px 0px #ff003c; color: #ff003c; }
          .toast.info { border-color: #00f0ff; box-shadow: 6px 6px 0px 0px #00f0ff; color: #00f0ff; }
          .toast.warning { border-color: #fcee0a; box-shadow: 6px 6px 0px 0px #fcee0a; color: #fcee0a; }
          
          /* === Responsive Design === */
          @media (max-width: 768px) {
            body { background-size: 30px 30px; }
            .cyber-card { 
              padding: 1.5rem; 
              box-shadow: 4px 4px 0px 0px #00f0ff; 
            }
            .glitch-text { font-size: 2.5rem !important; }
            #toast-container { right: 10px; left: 10px; top: 60px; }
            .toast { min-width: auto; max-width: 100%; }
            nav .hidden-mobile { display: none !important; }
          }
          
          @media (max-width: 640px) {
            .cyber-btn, .cyber-btn-secondary {
              padding: 10px 18px;
              font-size: 0.875rem;
            }
          }
          
          /* === Accessibility === */
          .sr-only {
            position: absolute;
            width: 1px;
            height: 1px;
            padding: 0;
            margin: -1px;
            overflow: hidden;
            clip: rect(0, 0, 0, 0);
            white-space: nowrap;
            border: 0;
          }
          
          :focus-visible {
            outline: 2px solid #fcee0a;
            outline-offset: 4px;
          }
        "]]
       [:body
        ;; Toast container
        [:div {:id "toast-container" :role "alert" :aria-live "polite"}]
        
        ;; Navigation
        [:nav {:class "bg-black border-b-2 border-cyber-cyan p-4 mb-8" :role "navigation" :aria-label "Main navigation"}
         [:div {:class "container mx-auto flex justify-between items-center flex-wrap gap-4"}
          [:a {:href "/" 
               :class "text-xl md:text-2xl font-black italic tracking-tighter text-cyber-yellow drop-shadow-[2px_2px_0px_#ff003c]"
               :aria-label "Home - Mindward Simplicity"} 
           "MINDWARD // SIMPLICITY"]
          [:div {:class "flex items-center space-x-4 md:space-x-6 uppercase tracking-widest text-xs md:text-sm"}
           [:a {:href "/leaderboard" 
                :class "text-gray-400 hover:text-cyber-cyan transition-colors"
                :aria-label "View leaderboard"} 
            "Leaderboard"]
           (if username
             [:div {:class "flex items-center space-x-2 md:space-x-4"}
              [:span {:class "text-gray-500 hidden-mobile"} "PILOT: " [:span {:class "font-bold text-cyber-cyan"} username]]
              [:a {:href "/game" 
                   :class "text-cyber-yellow hover:text-white hover:drop-shadow-[0_0_5px_#fcee0a]"
                   :aria-label "Start game"} 
               "JACK IN"]
              [:a {:href "/logout" 
                   :class "text-cyber-red hover:text-white"
                   :aria-label "Logout"} 
               "ABORT"]]
             [:div {:class "space-x-2 md:space-x-4"}
              [:a {:href "/login" 
                   :class "text-gray-400 hover:text-cyber-cyan"
                   :aria-label "Login"} 
               "LOGIN"]
              [:a {:href "/signup" 
                   :class "cyber-btn text-xs px-3 md:px-4 py-2"
                   :aria-label "Sign up"} 
               "INITIATE"]])]]]
        
        ;; Main content
        [:main {:class "container mx-auto px-4" :role "main"}
         content]
        
        ;; Footer/extra
        extra-footer
        
        ;; Toast notification script
        [:script "
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
          }
          
          // Form validation and loading states
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
          });
        "]]]))))

(defn leaderboard-page [session leaderboard]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Global Leaderboard"
                 [:div {:class "max-w-4xl mx-auto fade-in"}
                  [:h1 {:class "text-3xl md:text-5xl font-black text-cyber-yellow mb-8 md:mb-12 text-center uppercase tracking-widest glitch-text"} 
                   "Netrunner Legends"]
                  
                  [:div {:class "bg-black border border-cyber-cyan relative overflow-hidden"}
                   ;; Corner decorations
                   [:div {:class "absolute -top-1 -left-1 w-4 h-4 border-t-2 border-l-2 border-cyber-yellow"}]
                   [:div {:class "absolute -top-1 -right-1 w-4 h-4 border-t-2 border-r-2 border-cyber-yellow"}]
                   [:div {:class "absolute -bottom-1 -left-1 w-4 h-4 border-b-2 border-l-2 border-cyber-yellow"}]
                   [:div {:class "absolute -bottom-1 -right-1 w-4 h-4 border-b-2 border-r-2 border-cyber-yellow"}]

                   ;; Responsive table wrapper
                   [:div {:class "overflow-x-auto"}
                    [:table {:class "w-full text-left border-collapse" :role "table" :aria-label "Leaderboard"}
                     [:thead {:class "bg-zinc-900 text-cyber-cyan uppercase text-xs md:text-sm border-b-2 border-cyber-red"}
                      [:tr {:role "row"}
                       [:th {:class "px-3 md:px-6 py-3 md:py-4 font-bold tracking-wider" :role "columnheader"} "Rank"]
                       [:th {:class "px-3 md:px-6 py-3 md:py-4 font-bold tracking-wider" :role "columnheader"} "Netrunner"]
                       [:th {:class "px-3 md:px-6 py-3 md:py-4 text-right font-bold tracking-wider" :role "columnheader"} "Score"]]]
                     [:tbody
                      (if (empty? leaderboard)
                        [:tr {:role "row"} 
                         [:td {:colspan 3 
                               :class "px-6 py-10 text-center text-gray-500 uppercase tracking-widest text-xs md:text-sm"} 
                          "No data in the net."]]
                        (map-indexed
                         (fn [idx {:keys [username name high_score]}]
                           [:tr {:class (str "border-b border-zinc-800 hover:bg-zinc-900 transition-colors " 
                                            (when (= idx 0) "text-cyber-yellow font-bold "))
                                 :role "row"
                                 :style (str "animation-delay: " (* idx 0.05) "s")}
                            [:td {:class "px-3 md:px-6 py-3 md:py-4 font-mono text-sm md:text-base" :role "cell"} 
                             (cond
                               (= idx 0) "👑 KING"
                               (= idx 1) "🥈 02"
                               (= idx 2) "🥉 03"
                               :else (format "%02d" (inc idx)))]
                            [:td {:class "px-3 md:px-6 py-3 md:py-4 text-sm md:text-base truncate max-w-xs" :role "cell"} 
                             (or name username)]
                            [:td {:class "px-3 md:px-6 py-3 md:py-4 text-right font-mono text-base md:text-lg" :role "cell"} 
                             high_score]])
                         leaderboard))]]]]
                  
                  ;; Back to game button (if logged in)
                  (when (:user session)
                    [:div {:class "mt-8 text-center"}
                     [:a {:href "/game" :class "cyber-btn-secondary inline-block"} "Return to Combat"]])])})

(defn signup-page [_session params anti-forgery-token]
  {:status 200
   :headers {"Content-Type" "text/html"}
   ;; Note: Pass nil for session - signup page is for unauthenticated users only
   :body (layout nil "Join the Fleet"
                 [:div {:class "max-w-md mx-auto cyber-card mt-8 md:mt-12 fade-in"}
                  [:h2 {:class "text-2xl md:text-3xl font-black mb-6 md:mb-8 text-center text-cyber-yellow uppercase tracking-widest"} 
                   "Identity Reg"]
                  
                  (when (:error params)
                    [:div {:class "bg-red-900/50 border-l-4 border-cyber-red text-red-200 px-4 py-3 mb-6 font-mono text-xs md:text-sm slide-in" 
                           :role "alert"
                           :aria-live "assertive"}
                     [:span {:class "sr-only"} "Error: "]
                     "ERROR: IDENTITY CONFLICT DETECTED."])
                  
                  [:form {:method "POST" 
                          :action "/signup" 
                          :id "signup-form"
                          :aria-label "Sign up form"
                          :novalidate true}
                   [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
                   
                   ;; Display Name
                   [:div {:class "mb-6"}
                    [:label {:for "name" 
                             :class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} 
                     "Handle (Display)"]
                    [:input {:type "text" 
                             :id "name"
                             :name "name" 
                             :class "w-full px-4 py-3 cyber-input" 
                             :placeholder "ENTER_HANDLE" 
                             :required true
                             :minlength "3"
                             :maxlength "50"
                             :autocomplete "name"
                             :aria-required "true"
                             :aria-describedby "name-help"}]
                    [:p {:id "name-help" :class "text-xs text-gray-500 mt-1"} "3-50 characters"]]
                   
                   ;; Username
                   [:div {:class "mb-6"}
                    [:label {:for "username"
                             :class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} 
                     "Net ID (Login)"]
                    [:input {:type "text" 
                             :id "username"
                             :name "username" 
                             :class "w-full px-4 py-3 cyber-input" 
                             :placeholder "ENTER_ID" 
                             :required true
                             :minlength "3"
                             :maxlength "20"
                             :pattern "[a-zA-Z0-9_-]+"
                             :autocomplete "username"
                             :aria-required "true"
                             :aria-describedby "username-help"}]
                    [:p {:id "username-help" :class "text-xs text-gray-500 mt-1"} "3-20 characters, alphanumeric only"]]
                   
                   ;; Password
                   [:div {:class "mb-8"}
                    [:label {:for "password"
                             :class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} 
                     "Access Key (Password)"]
                    [:input {:type "password" 
                             :id "password"
                             :name "password" 
                             :class "w-full px-4 py-3 cyber-input" 
                             :placeholder "********" 
                             :required true
                             :minlength "8"
                             :autocomplete "new-password"
                             :aria-required "true"
                             :aria-describedby "password-help"}]
                    [:p {:id "password-help" :class "text-xs text-gray-500 mt-1"} "Minimum 8 characters"]]
                   
                   ;; Submit Button
                   [:button {:type "submit" 
                             :class "w-full cyber-btn"
                             :aria-label "Create account"} 
                    "ESTABLISH LINK"]
                   
                   ;; Login Link
                   [:div {:class "mt-6 text-center text-xs md:text-sm"}
                    [:span {:class "text-gray-500"} "Already registered? "]
                    [:a {:href "/login" :class "text-cyber-cyan hover:text-cyber-yellow transition-colors"} "Login here"]]]
                  
                  ;; Client-side validation script
                  [:script "
                    const form = document.getElementById('signup-form');
                    if (form) {
                      form.addEventListener('submit', (e) => {
                        const username = form.querySelector('[name=username]');
                        const password = form.querySelector('[name=password]');
                        const name = form.querySelector('[name=name]');
                        
                        // Username validation
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
                        
                        // Password validation
                        if (password.value.length < 8) {
                          e.preventDefault();
                          showToast('Access Key must be at least 8 characters', 'error');
                          password.focus();
                          return;
                        }
                        
                        // Name validation
                        if (name.value.length < 3) {
                          e.preventDefault();
                          showToast('Handle must be at least 3 characters', 'error');
                          name.focus();
                          return;
                        }
                      });
                    }
                  "]])})

(defn game-page [_session anti-forgery-token high-score]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str
          (h/html
           [:html {:lang "en"}
            [:head
             [:meta {:charset "UTF-8"}]
             [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
             [:title "Space Shooter - Simplicity"]
             [:script {:src "https://cdn.tailwindcss.com"}]
             [:link {:href "https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&display=swap" :rel "stylesheet"}]
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


(defn landing-page [session]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Welcome"
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
                    [:a {:href "/login" 
                         :class "cyber-btn-secondary min-w-[150px] text-center"
                         :aria-label "Login to existing account"} 
                     "Login"]
                    [:a {:href "/signup" 
                         :class "cyber-btn min-w-[150px] text-center"
                         :aria-label "Create new account"} 
                     "Initiate"]]]

                  [:script {:src "/js/life.js"}]])})

(defn login-page [session params anti-forgery-token]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Login"
                 [:div {:class "max-w-md mx-auto cyber-card mt-8 md:mt-12 fade-in"}
                  [:h2 {:class "text-2xl md:text-3xl font-black mb-6 md:mb-8 text-center text-cyber-yellow uppercase tracking-widest"} 
                   "Net Access"]
                  
                  (when (:error params)
                    [:div {:class "bg-red-900/50 border-l-4 border-cyber-red text-red-200 px-4 py-3 mb-6 font-mono text-xs md:text-sm slide-in"
                           :role "alert"
                           :aria-live "assertive"}
                     [:span {:class "sr-only"} "Error: "]
                     "ACCESS DENIED. INVALID CREDENTIALS."])
                  
                  [:form {:method "POST" 
                          :action "/login"
                          :id "login-form"
                          :aria-label "Login form"}
                   [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
                   
                   ;; Username
                   [:div {:class "mb-6"}
                    [:label {:for "username-input"
                             :class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} 
                     "Net ID"]
                    [:input {:type "text" 
                             :id "username-input"
                             :name "username"
                             :class "w-full px-4 py-3 cyber-input"
                             :placeholder "ID_REQUIRED"
                             :required true
                             :autocomplete "username"
                             :aria-required "true"
                             :autofocus true}]]
                   
                   ;; Password
                   [:div {:class "mb-8"}
                    [:label {:for "password-input"
                             :class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} 
                     "Access Key"]
                    [:input {:type "password" 
                             :id "password-input"
                             :name "password"
                             :class "w-full px-4 py-3 cyber-input"
                             :placeholder "KEY_REQUIRED"
                             :required true
                             :autocomplete "current-password"
                             :aria-required "true"}]]
                   
                   ;; Submit Button
                   [:button {:type "submit"
                             :class "w-full cyber-btn"
                             :aria-label "Login"}
                    "JACK IN"]
                   
                   ;; Signup Link
                   [:div {:class "mt-6 text-center text-xs md:text-sm"}
                    [:span {:class "text-gray-500"} "New to the grid? "]
                    [:a {:href "/signup" :class "text-cyber-cyan hover:text-cyber-yellow transition-colors"} "Register here"]]]
                  
                  ;; Show toast on error
                  (when (:error params)
                    [:script "setTimeout(() => showToast('Invalid credentials. Try again.', 'error'), 100);"])])})
