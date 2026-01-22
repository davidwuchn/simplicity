(ns cc.mindward.ui.core
  (:require [hiccup2.core :as h]))

(defn layout [session title content & [extra-footer]]
  (let [username (:username session)]
    (str
     (h/html
      [:html
       [:head
        [:meta {:charset "UTF-8"}]
        [:title title]
        [:script {:src "https://cdn.tailwindcss.com"}]
        [:link {:href "https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&display=swap" :rel "stylesheet"}]
        [:style "
          body { background-color: #050505; color: #e2e8f0; font-family: 'Orbitron', sans-serif; background-image: linear-gradient(0deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent); background-size: 50px 50px; }
          .font-cyber { font-family: 'Orbitron', sans-serif; }
          .cyber-card { background-color: #000; border: 2px solid #fcee0a; box-shadow: 6px 6px 0px 0px #00f0ff; }
          .cyber-input { background-color: #1a1a1a; border: 1px solid #00f0ff; color: #00f0ff; border-radius: 0; }
          .cyber-input:focus { border-color: #fcee0a; outline: none; box-shadow: 0 0 10px #fcee0a; }
          .cyber-btn { background-color: #fcee0a; color: #000; font-weight: 900; text-transform: uppercase; border: none; clip-path: polygon(10% 0, 100% 0, 100% 70%, 90% 100%, 0 100%, 0 30%); padding: 12px 24px; transition: all 0.2s; }
          .cyber-btn:hover { background-color: #00f0ff; box-shadow: 4px 4px 0px #ff003c; transform: translate(-2px, -2px); }
          .cyber-btn-secondary { background-color: #2a2a2a; color: #00f0ff; border: 2px solid #00f0ff; font-weight: 700; text-transform: uppercase; clip-path: polygon(10% 0, 100% 0, 100% 70%, 90% 100%, 0 100%, 0 30%); padding: 10px 22px; transition: all 0.2s; }
          .cyber-btn-secondary:hover { background-color: #00f0ff; color: #000; box-shadow: 4px 4px 0px #fcee0a; }
          .glitch-text { text-shadow: 2px 0 #ff003c, -2px 0 #00f0ff; animation: glitch 1s infinite alternate-reverse; }
          @keyframes glitch { 0% { text-shadow: 2px 0 #ff003c, -2px 0 #00f0ff; } 25% { text-shadow: -2px 0 #ff003c, 2px 0 #00f0ff; } 50% { text-shadow: 2px 0 #00f0ff, -2px 0 #fcee0a; } 100% { text-shadow: -2px 0 #00f0ff, 2px 0 #ff003c; } }
          .text-cyber-yellow { color: #fcee0a; }
          .text-cyber-cyan { color: #00f0ff; }
          .text-cyber-red { color: #ff003c; }
          .border-cyber-yellow { border-color: #fcee0a; }
          .border-cyber-cyan { border-color: #00f0ff; }
        "]]
       [:body
        [:nav {:class "bg-black border-b-2 border-cyber-cyan p-4 mb-8"}
         [:div {:class "container mx-auto flex justify-between items-center"}
          [:a {:href "/" :class "text-2xl font-black italic tracking-tighter text-cyber-yellow drop-shadow-[2px_2px_0px_#ff003c]"} "MINDWARD // SIMPLICITY"]
          [:div {:class "flex items-center space-x-6 uppercase tracking-widest text-sm"}
           [:a {:href "/leaderboard" :class "text-gray-400 hover:text-cyber-cyan transition-colors"} "Leaderboard"]
           (if username
             [:div {:class "flex items-center space-x-4"}
              [:span {:class "text-gray-500"} "PILOT: " [:span {:class "font-bold text-cyber-cyan"} username]]
              [:a {:href "/game" :class "text-cyber-yellow hover:text-white hover:drop-shadow-[0_0_5px_#fcee0a]"} "JACK IN"]
              [:a {:href "/logout" :class "text-cyber-red hover:text-white"} "ABORT"]]
             [:div {:class "space-x-4"}
              [:a {:href "/login" :class "text-gray-400 hover:text-cyber-cyan"} "LOGIN"]
              [:a {:href "/signup" :class "cyber-btn text-xs px-4 py-2"} "INITIATE"]])] ]]
        [:main {:class "container mx-auto px-4"}
         content]
        extra-footer]]))))

(defn leaderboard-page [session leaderboard]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Global Leaderboard"
                 [:div {:class "max-w-4xl mx-auto"}
                  [:h1 {:class "text-5xl font-black text-cyber-yellow mb-12 text-center uppercase tracking-widest glitch-text"} "Netrunner Legends"]
                  [:div {:class "bg-black border border-cyber-cyan relative"}
                   [:div {:class "absolute -top-1 -left-1 w-4 h-4 border-t-2 border-l-2 border-cyber-yellow"}]
                   [:div {:class "absolute -top-1 -right-1 w-4 h-4 border-t-2 border-r-2 border-cyber-yellow"}]
                   [:div {:class "absolute -bottom-1 -left-1 w-4 h-4 border-b-2 border-l-2 border-cyber-yellow"}]
                   [:div {:class "absolute -bottom-1 -right-1 w-4 h-4 border-b-2 border-r-2 border-cyber-yellow"}]
                   
                   [:table {:class "w-full text-left border-collapse"}
                    [:thead {:class "bg-zinc-900 text-cyber-cyan uppercase text-sm border-b-2 border-cyber-red"}
                     [:tr
                      [:th {:class "px-6 py-4 font-bold tracking-wider"} "Rank"]
                      [:th {:class "px-6 py-4 font-bold tracking-wider"} "Netrunner"]
                      [:th {:class "px-6 py-4 text-right font-bold tracking-wider"} "Score"]]]
                    [:tbody
                     (if (empty? leaderboard)
                       [:tr [:td {:colspan 3 :class "px-6 py-10 text-center text-gray-500 uppercase tracking-widest"} "No data in the net."]]
                       (map-indexed 
                        (fn [idx {:keys [username name high_score]}]
                          [:tr {:class (str "border-b border-zinc-800 hover:bg-zinc-900 transition-colors " (when (= idx 0) "text-cyber-yellow font-bold"))}
                           [:td {:class "px-6 py-4 font-mono"} (if (= idx 0) "KING" (format "%02d" (inc idx)))]
                           [:td {:class "px-6 py-4"} (or name username)]
                           [:td {:class "px-6 py-4 text-right font-mono text-lg"} high_score]])
                        leaderboard))]]]])})

(defn signup-page [session params anti-forgery-token]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Join the Fleet"
                 [:div {:class "max-w-md mx-auto cyber-card mt-12"}
                  [:h2 {:class "text-3xl font-black mb-8 text-center text-cyber-yellow uppercase tracking-widest"} "Identity Reg"]
                  (when (:error params)
                    [:div {:class "bg-red-900/50 border-l-4 border-cyber-red text-red-200 px-4 py-3 mb-6 font-mono text-sm"}
                     "ERROR: IDENTITY CONFLICT DETECTED."])
                  [:form {:method "POST" :action "/signup"}
                   [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
                   [:div {:class "mb-6"}
                    [:label {:class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} "Handle (Display)"]
                    [:input {:type "text" :name "name" :class "w-full px-4 py-3 cyber-input" :placeholder "ENTER_HANDLE" :required true}]]
                   [:div {:class "mb-6"}
                    [:label {:class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} "Net ID (Login)"]
                    [:input {:type "text" :name "username" :class "w-full px-4 py-3 cyber-input" :placeholder "ENTER_ID" :required true}]]
                   [:div {:class "mb-8"}
                    [:label {:class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} "Access Key (Password)"]
                    [:input {:type "password" :name "password" :class "w-full px-4 py-3 cyber-input" :placeholder "********" :required true}]]
                   [:button {:type "submit" :class "w-full cyber-btn"} "ESTABLISH LINK"]]])})

(defn game-page [session anti-forgery-token high-score]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Space Shooter"
                 [:div {:class "max-w-5xl mx-auto"}
                  [:input {:type "hidden" :id "csrf-token" :value anti-forgery-token}]
                  [:div {:class "flex justify-between items-end mb-6 border-b-2 border-cyber-red pb-2"}
                   [:h1 {:class "text-4xl font-black text-cyber-yellow italic"} "NEON // SHOOTER"]
                   [:div {:class "text-xl font-mono text-cyber-cyan"} "BEST_RUN: " [:span {:id "high-score" :class "text-white"} high-score]]]
                  
                  [:div {:class "relative p-1 bg-gradient-to-br from-cyber-yellow to-cyber-red"}
                   [:canvas {:id "gameCanvas" :width "800" :height "600" :class "bg-black block mx-auto cursor-crosshair"}]]
                  
                  [:div {:class "mt-8 grid grid-cols-2 gap-12 text-gray-400"}
                   [:div {:class "border-l-2 border-cyber-cyan pl-4"}
                    [:h3 {:class "font-bold text-cyber-cyan mb-3 uppercase tracking-widest"} "System Controls"]
                    [:ul {:class "space-y-2 font-mono text-sm"}
                     [:li "ARROWS // MANEUVER"]
                     [:li "SPACE  // DISCHARGE"]
                     [:li "R_KEY  // SYSTEM_REBOOT"]]]
                   [:div {:class "border-l-2 border-cyber-yellow pl-4"}
                    [:h3 {:class "font-bold text-cyber-yellow mb-3 uppercase tracking-widest"} "Tactical Data"]
                    [:ul {:class "space-y-2 font-mono text-sm"}
                     [:li ">> THREATS FROM ALL VECTORS"]
                     [:li ">> ACQUIRE OVERCLOCK MODULES"]]]]]
                 [:script {:src (str "/js/game.js?v=" (System/currentTimeMillis))}])})

(defn landing-page [session]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Welcome"
                 [:div {:class "min-h-[80vh] flex flex-col justify-center items-center text-center"}
                  [:div {:class "relative mb-12"}
                   [:h1 {:class "text-7xl font-black text-cyber-yellow mb-2 glitch-text uppercase tracking-tighter"} "MINDWARD"]
                   [:div {:class "text-2xl font-bold text-cyber-cyan tracking-[1em] uppercase"} "Simplicity"]]
                  
                  [:p {:class "text-xl text-gray-400 mb-16 max-w-lg font-mono border-l-4 border-cyber-red pl-6 text-left"} 
                   "Connect to the grid. Engage hostile protocols. Ascend the hierarchy."]
                  
                  [:div {:class "flex space-x-8"}
                   [:a {:href "/login" :class "cyber-btn-secondary min-w-[150px]"} "Login"]
                   [:a {:href "/signup" :class "cyber-btn min-w-[150px]"} "Initiate"]]])})

(defn login-page [session params anti-forgery-token]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Login"
                 [:div {:class "max-w-md mx-auto cyber-card mt-12"}
                  [:h2 {:class "text-3xl font-black mb-8 text-center text-cyber-yellow uppercase tracking-widest"} "Net Access"]
                  (when (:error params)
                    [:div {:class "bg-red-900/50 border-l-4 border-cyber-red text-red-200 px-4 py-3 mb-6 font-mono text-sm"}
                     "ACCESS DENIED. INVALID CREDENTIALS."])
                  [:form {:method "POST" :action "/login"}
                   [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
                   [:div {:class "mb-6"}
                    [:label {:class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} "Net ID"]
                    [:input {:type "text" :name "username"
                             :class "w-full px-4 py-3 cyber-input"
                             :placeholder "ID_REQUIRED"
                             :required true}]]
                   [:div {:class "mb-8"}
                    [:label {:class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"} "Access Key"]
                    [:input {:type "password" :name "password"
                             :class "w-full px-4 py-3 cyber-input"
                             :placeholder "KEY_REQUIRED"
                             :required true}]]
                   [:button {:type "submit"
                             :class "w-full cyber-btn"}
                    "JACK IN"]]])})
