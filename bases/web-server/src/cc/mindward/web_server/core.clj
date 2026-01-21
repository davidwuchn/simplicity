(ns cc.mindward.web-server.core
  (:require [cc.mindward.auth.interface :as auth]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [hiccup2.core :as h]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as res]
            [cc.mindward.user.interface :as user])
  (:gen-class))

(defn layout [session title content & [extra-footer]]
  (let [username (:username session)]
    (str
     (h/html
      [:html
       [:head
        [:meta {:charset "UTF-8"}]
        [:title title]
        [:script {:src "https://cdn.tailwindcss.com"}]
        [:style "body { background-color: #0f172a; color: #f8fafc; } .bg-white { background-color: #1e293b; border-color: #334155; } .text-gray-600 { color: #94a3b8; } .text-indigo-600 { color: #818cf8; }"]]
       [:body
        [:nav {:class "bg-slate-900 border-b border-slate-800 p-4 mb-8"}
         [:div {:class "container mx-auto flex justify-between items-center"}
          [:a {:href "/" :class "text-xl font-bold text-indigo-400"} "Mindward Simplicity"]
          [:div {:class "flex items-center space-x-6"}
           [:a {:href "/leaderboard" :class "text-gray-300 hover:text-indigo-400"} "Leaderboard"]
           (if username
             [:div {:class "flex items-center space-x-4"}
              [:span {:class "text-gray-300"} "Welcome, " [:span {:class "font-bold text-white"} username]]
              [:a {:href "/game" :class "text-indigo-400 hover:text-indigo-300"} "Play Game"]
              [:a {:href "/logout" :class "text-red-400 hover:text-red-500"} "Logout"]]
             [:div {:class "space-x-4"}
              [:a {:href "/login" :class "text-gray-300 hover:text-indigo-400"} "Login"]
              [:a {:href "/signup" :class "bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"} "Sign Up"]])] ]]
        [:main {:class "container mx-auto px-4"}
         content]
        extra-footer]]))))

(defn leaderboard-page [{:keys [session]}]
  (let [leaderboard (user/get-leaderboard)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (layout session "Global Leaderboard"
                   [:div {:class "max-w-2xl mx-auto"}
                    [:h1 {:class "text-4xl font-bold text-indigo-400 mb-8 text-center"} "Galactic Heroes"]
                    [:div {:class "bg-slate-800 rounded-xl shadow-2xl overflow-hidden"}
                     [:table {:class "w-full text-left"}
                      [:thead {:class "bg-slate-700 text-gray-300 uppercase text-sm"}
                       [:tr
                        [:th {:class "px-6 py-4"} "Rank"]
                        [:th {:class "px-6 py-4"} "Pilot"]
                        [:th {:class "px-6 py-4 text-right"} "High Score"]]]
                      [:tbody
                       (if (empty? leaderboard)
                         [:tr [:td {:colspan 3 :class "px-6 py-10 text-center text-gray-500"} "No scores recorded yet. Be the first!"]]
                         (map-indexed 
                          (fn [idx {:keys [username name high_score]}]
                            [:tr {:class (str "border-b border-slate-700 hover:bg-slate-700/50 " (when (= idx 0) "text-yellow-400 font-bold"))}
                             [:td {:class "px-6 py-4"} (inc idx)]
                             [:td {:class "px-6 py-4"} (or name username)]
                             [:td {:class "px-6 py-4 text-right font-mono"} high_score]])
                          leaderboard))]]]])}))

(defn signup-page [request]
  (let [{:keys [session params anti-forgery-token]} request]
    (if (:username session)
      (res/redirect "/game")
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (layout session "Join the Fleet"
                     [:div {:class "max-w-md mx-auto bg-white p-8 rounded-xl shadow-md"}
                      [:h2 {:class "text-2xl font-bold mb-6 text-center text-white"} "Create your Pilot Profile"]
                      (when (:error params)
                        [:div {:class "bg-red-900 border border-red-400 text-red-200 px-4 py-3 rounded mb-4"}
                         "Username already taken or invalid data."])
                      [:form {:method "POST" :action "/signup"}
                       [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
                       [:div {:class "mb-4"}
                        [:label {:class "block text-gray-300 text-sm font-bold mb-2"} "Pilot Name (Display)"]
                        [:input {:type "text" :name "name" :class "w-full px-3 py-2 border border-slate-700 bg-slate-800 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500" :required true}]]
                       [:div {:class "mb-4"}
                        [:label {:class "block text-gray-300 text-sm font-bold mb-2"} "Username (Login)"]
                        [:input {:type "text" :name "username" :class "w-full px-3 py-2 border border-slate-700 bg-slate-800 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500" :required true}]]
                       [:div {:class "mb-6"}
                        [:label {:class "block text-gray-300 text-sm font-bold mb-2"} "Security Code (Password)"]
                        [:input {:type "password" :name "password" :class "w-full px-3 py-2 border border-slate-700 bg-slate-800 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500" :required true}]]
                       [:button {:type "submit" :class "w-full bg-indigo-600 text-white py-2 rounded-lg font-bold hover:bg-indigo-700 transition duration-200"} "Enlist Now"]]])})))

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
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body (layout session "Space Shooter"
                       [:div {:class "max-w-4xl mx-auto"}
                        [:input {:type "hidden" :id "csrf-token" :value anti-forgery-token}]
                        [:div {:class "flex justify-between items-center mb-4"}
                         [:h1 {:class "text-3xl font-bold text-indigo-400"} "Space Shooter"]
                         [:div {:class "text-xl"} "High Score: " [:span {:id "high-score"} high-score]]]
                        [:canvas {:id "gameCanvas" :width "800" :height "600" :class "bg-black border-4 border-slate-700 rounded-lg shadow-2xl block mx-auto"}]
                        [:div {:class "mt-6 grid grid-cols-2 gap-8 text-slate-400"}
                         [:div
                          [:h3 {:class "font-bold text-white mb-2"} "Controls"]
                          [:ul {:class "list-disc list-inside"}
                           [:li "Arrows: Move ship"]
                           [:li "Space: Fire lasers"]]]
                         [:div
                          [:h3 {:class "font-bold text-white mb-2"} "Tips"]
                          [:ul {:class "list-disc list-inside"}
                           [:li "Enemies can come from above or the sides!"]
                           [:li "Collect power-ups for faster firing!"]]]]]
                       [:script (h/raw "
                        const canvas = document.getElementById('gameCanvas');
                        const ctx = canvas.getContext('2d');
                        const highScoreEl = document.getElementById('high-score');
                        let score = 0; let gameOver = false; let powerUpActive = false; let powerUpTimer = 0;
                        const player = { x: canvas.width / 2, y: canvas.height - 50, size: 30, speed: 5, color: '#818cf8' };
                        const keys = {};
                        window.addEventListener('keydown', e => keys[e.code] = true);
                        window.addEventListener('keyup', e => keys[e.code] = false);
                        let bullets = []; let enemies = []; let powerUps = []; let particles = []; let enemyBullets = [];
                        let lastBossSpawnScore = 0;
                        let audioCtx;
                        function initAudio() { if (!audioCtx) { audioCtx = new (window.AudioContext || window.webkitAudioContext)(); startBGM(); } }
                        function playSound(type) {
                            if (!audioCtx) return;
                            const osc = audioCtx.createOscillator(); const gain = audioCtx.createGain();
                            if (type === 'shoot') { osc.type = 'square'; osc.frequency.setValueAtTime(880, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(110, audioCtx.currentTime + 0.1); gain.gain.setValueAtTime(0.05, audioCtx.currentTime); }
                            else if (type === 'explosion') { osc.type = 'sawtooth'; osc.frequency.setValueAtTime(100, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5); gain.gain.setValueAtTime(0.1, audioCtx.currentTime); }
                            else if (type === 'boss-shoot') { osc.type = 'sine'; osc.frequency.setValueAtTime(440, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(220, audioCtx.currentTime + 0.2); gain.gain.setValueAtTime(0.05, audioCtx.currentTime); }
                            osc.connect(gain); gain.connect(audioCtx.destination);
                            osc.start(); osc.stop(audioCtx.currentTime + (type === 'explosion' ? 0.5 : 0.1));
                        }
                        function startBGM() {
                            const notes = [110, 123.47, 130.81, 146.83]; let noteIndex = 0;
                            setInterval(() => {
                                if (gameOver || !audioCtx) return;
                                const osc = audioCtx.createOscillator(); const gain = audioCtx.createGain();
                                osc.type = 'triangle'; osc.frequency.setValueAtTime(notes[noteIndex], audioCtx.currentTime);
                                gain.gain.setValueAtTime(0.03, audioCtx.currentTime); gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.4);
                                osc.connect(gain); gain.connect(audioCtx.destination);
                                osc.start(); osc.stop(audioCtx.currentTime + 0.4); noteIndex = (noteIndex + 1) % notes.length;
                            }, 500);
                        }
                        function spawnEnemy(isBoss = false) {
                            if (gameOver) return;
                            const side = Math.random(); let x, y, vx, vy;
                            if (side < 0.5) { x = Math.random() * canvas.width; y = -30; vx = (Math.random() - 0.5) * 2; vy = 2 + Math.random() * 2; }
                            else if (side < 0.75) { x = -30; y = Math.random() * (canvas.height / 2); vx = 2 + Math.random() * 2; vy = (Math.random() - 0.5) * 2; }
                            else { x = canvas.width + 30; y = Math.random() * (canvas.height / 2); vx = -(2 + Math.random() * 2); vy = (Math.random() - 0.5) * 2; }
                            if (isBoss) { x = canvas.width / 2; y = -100; vx = 0; vy = 1; }
                            enemies.push({ 
                                x, y, vx, vy, size: isBoss ? 80 : 20 + Math.random() * 10, color: isBoss ? '#f59e0b' : '#f87171', 
                                hp: isBoss ? 15 : 1, isBoss, lastShot: 0, lastTurn: Date.now(), turnRate: 1000 + Math.random() * 2000 
                            });
                        }
                        function update() {
                            if (gameOver) return;
                            if (keys['ArrowUp'] && player.y > 0) player.y -= player.speed;
                            if (keys['ArrowDown'] && player.y < canvas.height - player.size) player.y += player.speed;
                            if (keys['ArrowLeft'] && player.x > 0) player.x -= player.speed;
                            if (keys['ArrowRight'] && player.x < canvas.width - player.size) player.x += player.speed;
                            if (keys['Space']) {
                                initAudio(); const cooldown = powerUpActive ? 100 : 250;
                                if (!player.lastShotTime || Date.now() - player.lastShotTime > cooldown) { 
                                    bullets.push({ x: player.x + player.size/2, y: player.y, vy: -7 }); player.lastShotTime = Date.now(); playSound('shoot');
                                }
                            }
                            if (powerUpActive && Date.now() > powerUpTimer) powerUpActive = false;
                            bullets = bullets.filter(b => (b.y -= 7) > 0);
                            enemyBullets = enemyBullets.filter(b => {
                                b.x += b.vx; b.y += b.vy;
                                const dx = b.x - (player.x + player.size/2), dy = b.y - (player.y + player.size/2);
                                if (Math.sqrt(dx*dx + dy*dy) < 5 + player.size/2) { gameOver = true; saveScore(); }
                                return b.y < canvas.height && b.y > 0 && b.x > 0 && b.x < canvas.width;
                            });
                            enemies.forEach((e, ei) => {
                                if (e.isBoss) {
                                    if (e.y < 100) e.y += e.vy;
                                    else { e.x += Math.sin(Date.now()/500) * 2; }
                                    if (Date.now() - e.lastShot > 1200) {
                                        const angle = Math.atan2(player.y - e.y, player.x - e.x);
                                        enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * 4, vy: Math.sin(angle) * 4 });
                                        e.lastShot = Date.now(); playSound('boss-shoot');
                                    }
                                } else {
                                    if (Date.now() - e.lastTurn > e.turnRate) { e.vx += (Math.random() - 0.5) * 2; e.vy += (Math.random() - 0.5) * 1; e.lastTurn = Date.now(); }
                                    e.x += e.vx; e.y += e.vy;
                                }
                                const dx = e.x - (player.x + player.size/2), dy = e.y - (player.y + player.size/2);
                                if (Math.sqrt(dx*dx + dy*dy) < e.size/2 + player.size/2) { gameOver = true; saveScore(); }
                                bullets.forEach((b, bi) => {
                                    const bdx = b.x - e.x, bdy = b.y - e.y;
                                    if (Math.sqrt(bdx*bdx + bdy*bdy) < e.size/2) {
                                        bullets.splice(bi, 1); e.hp--;
                                        if (e.hp <= 0) {
                                            playSound('explosion'); enemies.splice(ei, 1); score += e.isBoss ? 100 : 10;
                                            if (Math.random() < 0.1) powerUps.push({ x: e.x, y: e.y, vy: 2, size: 15 });
                                        }
                                    }
                                });
                            });
                            powerUps = powerUps.filter(p => {
                                p.y += 2; const dx = p.x - (player.x + player.size/2), dy = p.y - (player.y + player.size/2);
                                if (Math.sqrt(dx*dx + dy*dy) < p.size + player.size/2) { powerUpActive = true; powerUpTimer = Date.now() + 5000; score += 50; return false; }
                                return p.y < canvas.height;
                            });
                            enemies = enemies.filter(e => e.y < canvas.height + 100 && e.x > -100 && e.x < canvas.width + 100);
                            if (Math.random() < 0.02) spawnEnemy();
                            if (score >= lastBossSpawnScore + 300 && !enemies.some(e => e.isBoss)) { spawnEnemy(true); lastBossSpawnScore = score; }
                        }
                        function draw() {
                            ctx.fillStyle = 'black'; ctx.fillRect(0, 0, canvas.width, canvas.height);
                            ctx.fillStyle = 'white'; for(let i=0; i<50; i++) ctx.fillRect((Math.sin(i*777) + 1) * canvas.width / 2, (Date.now() * 0.1 + i*100) % canvas.height, 1, 1);
                            ctx.fillStyle = powerUpActive ? '#fbbf24' : player.color; ctx.beginPath(); ctx.moveTo(player.x + player.size/2, player.y); ctx.lineTo(player.x, player.y + player.size); ctx.lineTo(player.x + player.size, player.y + player.size); ctx.fill();
                            ctx.fillStyle = '#60a5fa'; bullets.forEach(b => ctx.fillRect(b.x-2, b.y, 4, 10));
                            ctx.fillStyle = '#f87171'; enemyBullets.forEach(b => { ctx.beginPath(); ctx.arc(b.x, b.y, 6, 0, Math.PI*2); ctx.fill(); });
                            enemies.forEach(e => {
                                ctx.fillStyle = e.color; ctx.beginPath();
                                if (e.isBoss) { ctx.rect(e.x - e.size/2, e.y - e.size/2, e.size, e.size); ctx.fill(); ctx.fillStyle='white'; ctx.font = 'bold 16px Arial'; ctx.fillText('BOSS HP: ' + e.hp, e.x - 40, e.y - 50); }
                                else { ctx.arc(e.x, e.y, e.size/2, 0, Math.PI * 2); ctx.fill(); }
                            });
                            powerUps.forEach(p => { ctx.fillStyle = '#fbbf24'; ctx.fillRect(p.x - p.size/2, p.y - p.size/2, p.size, p.size); });
                            ctx.fillStyle = 'white'; ctx.font = '20px Arial'; ctx.fillText(`Score: ${score}`, 20, 30);
                            if (powerUpActive) { ctx.fillStyle = '#fbbf24'; ctx.fillText('RAPID FIRE ACTIVE!', 20, 60); }
                            if (gameOver) {
                                ctx.fillStyle = 'rgba(0,0,0,0.7)'; ctx.fillRect(0, 0, canvas.width, canvas.height);
                                ctx.fillStyle = 'white'; ctx.font = '50px Arial'; ctx.textAlign = 'center'; ctx.fillText('GAME OVER', canvas.width/2, canvas.height/2);
                                ctx.font = '25px Arial'; ctx.fillText(`Final Score: ${score}`, canvas.width/2, canvas.height/2 + 50); ctx.fillText('Press R to Restart', canvas.width/2, canvas.height/2 + 100);
                            }
                        }
                        function saveScore() {
                            const token = document.getElementById('csrf-token').value;
                            fetch('/game/score', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'x-csrf-token': token }, body: `score=${score}` })
                            .then(r => r.json()).then(data => { highScoreEl.innerText = data.highScore; });
                        }
                        function loop() { update(); draw(); requestAnimationFrame(loop); }
                        window.addEventListener('keydown', e => { 
                            if (e.code === 'KeyR' && gameOver) { score = 0; enemies = []; bullets = []; enemyBullets = []; powerUps = []; gameOver = false; lastBossSpawnScore = 0; player.x = canvas.width / 2; player.y = canvas.height - 50; }
                            initAudio();
                        });
                        loop();
                     ")])}))))
      (res/redirect "/login")

(defn save-score [{:keys [session params]}]
  (if-let [username (:username session)]
    (let [score (Integer/parseInt (str (or (:score params) "0")))]
      (user/update-high-score! username score)
      (let [new-high-score (user/get-high-score username)]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (str "{\"highScore\": " new-high-score "}")}))
    {:status 401}))

(defn landing-page [{:keys [session]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout session "Welcome"
                 [:div {:class "text-center py-20"}
                  [:h1 {:class "text-5xl font-extrabold text-white mb-4"} "Simplicity is the Ultimate Sophistication"]
                  [:p {:class "text-xl text-slate-400 mb-8"} "Now with Retro Space Shooter and SQLite persistence."]
                  (if (:username session)
                    [:div {:class "space-y-4"}
                     [:p {:class "text-indigo-400 font-bold text-2xl"} "Welcome back, " (:username session) "!"]
                     [:a {:href "/game" :class "inline-block bg-indigo-600 text-white px-8 py-3 rounded-lg font-semibold hover:bg-indigo-700"} "Launch Mission"]]
                    [:div {:class "space-x-4"}
                     [:a {:href "/login" :class "bg-indigo-600 text-white px-8 py-3 rounded-lg font-semibold hover:bg-indigo-700"} "Get Started"]
                     [:a {:href "/signup" :class "bg-slate-700 text-white px-8 py-3 rounded-lg font-semibold hover:bg-slate-600"} "Create Account"]])])})

(defn login-page [request]
  (let [{:keys [session params anti-forgery-token]} request]
    (if (:username session)
      (res/redirect "/game")
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (layout session "Login"
                     [:div {:class "max-w-md mx-auto bg-white p-8 rounded-xl shadow-md"}
                      [:h2 {:class "text-2xl font-bold mb-6 text-center text-white"} "Login to your account"]
                      (when (:error params)
                        [:div {:class "bg-red-900 border border-red-400 text-red-200 px-4 py-3 rounded mb-4"}
                         "Invalid username or password"])
                      [:form {:method "POST" :action "/login"}
                       [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]
                       [:div {:class "mb-4"}
                        [:label {:class "block text-gray-300 text-sm font-bold mb-2"} "Username"]
                        [:input {:type "text" :name "username" :class "w-full px-3 py-2 border border-slate-700 bg-slate-800 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500" :required true}]]
                       [:div {:class "mb-6"}
                        [:label {:class "block text-gray-300 text-sm font-bold mb-2"} "Password"]
                        [:input {:type "password" :name "password" :class "w-full px-3 py-2 border border-slate-700 bg-slate-800 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500" :required true}]]
                       [:button {:type "submit" :class "w-full bg-indigo-600 text-white py-2 rounded-lg font-bold hover:bg-indigo-700 transition duration-200"} "Sign In"]]])})))

(defn handle-login [{:keys [params session]}]
  (let [username (:username params)
        password (:password params)]
    (if (auth/authenticate username password)
      (-> (res/redirect "/game")
          (assoc :session (assoc session :username username)))
      (res/redirect "/login?error=true"))))

(defn handle-logout [{:keys [session]}]
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

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (println "Starting Retro Game Server on port" port "...")
    (jetty/run-jetty site-app {:port port :join? false})))