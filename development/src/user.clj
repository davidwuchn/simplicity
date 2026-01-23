(ns user
  "Development REPL environment with hot reload capabilities.
   
   Best Practices:
     - Lifecycle management with proper shutdown hooks
     - State preserved across reloads using defonce
     - Component initialization/suspension pattern
     - Development-only paths excluded from refresh
   
   Usage:
     (start)     - Start the web server with full initialization
     (stop)      - Stop server and cleanup all resources
     (restart)   - Stop, reload code, restart (hot reload)
     (reset)     - Reload changed namespaces without restart
     (status)    - Check system status with health checks
     (watch-on)  - Enable automatic file watching (2s interval)
     (watch-off) - Disable automatic file watching
   
   Philosophy (∃ Truth): 
     Hot reload should preserve application state while reloading logic.
     Proper lifecycle management prevents resource leaks."
  (:require [clojure.tools.namespace.repl :as tools-ns]
            [clojure.tools.namespace.track :as track]
            [clojure.tools.namespace.dir :as dir]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

;; ∀ Vigilance: Exclude development-only files from refresh to avoid errors
;; Do NOT include "development/src" - causes test-runner to load test deps
(tools-ns/set-refresh-dirs "components/auth/src"
                            "components/user/src"
                            "components/game/src"
                            "components/ui/src"
                            "bases/web-server/src")

;; ------------------------------------------------------------
;; System State Management (π Synthesis)
;; ------------------------------------------------------------
;; τ Wisdom: Use defonce to preserve state across hot reloads
;; φ Vitality: Atom holds live system components
;; System state preserves server and component lifecycle across reloads.
;; Structure: {:server <jetty> :components {:user ... :game ...} :watcher <future>}

(defonce ^:private system (atom nil))

;; ------------------------------------------------------------
;; File Watcher for Auto-Reload (φ Vitality)
;; ------------------------------------------------------------

(defn- auto-reload-loop
  "Background loop that checks for file changes and triggers hot reload.
   Uses tools.namespace's dir scanning to detect changes efficiently."
  [enabled-atom]
  (try
    (let [dirs (map io/file ["components/auth/src"
                             "components/user/src"
                             "components/game/src"
                             "components/ui/src"
                             "bases/web-server/src"])]
      (loop [last-tracker (track/tracker)]
        (when @enabled-atom
          (Thread/sleep 2000) ; Check every 2 seconds
          (let [next-tracker
                (try
                  (let [new-tracker (dir/scan-dirs last-tracker dirs)]
                    (if (seq (::track/load new-tracker))
                      (do
                        (log/info "🔍 File changes detected, auto-reloading...")
                        ;; Use resolve to avoid forward reference to (stop)
                        ((resolve 'user/stop))
                        (tools-ns/refresh :after 'user/start)
                        (track/tracker)) ; Reset tracker after reload
                      new-tracker))
                  (catch Exception e
                    (log/error e "Auto-reload error")
                    last-tracker))]
            (recur next-tracker)))))
    (catch Exception e
      (log/error e "Auto-reload loop crashed"))))

(defn start-watcher!
  "Start the file watcher for automatic hot reload.
   Returns a future that can be cancelled with (future-cancel ...)."
  []
  (let [enabled (atom true)
        watcher-future (future (auto-reload-loop enabled))]
    {:enabled enabled
     :future watcher-future}))

(defn stop-watcher!
  "Stop the file watcher."
  [watcher]
  (when watcher
    (reset! (:enabled watcher) false)
    (future-cancel (:future watcher))))

(defn- shutdown-components!
  "Properly shutdown all initialized components.
   π (Synthesis): Reverse dependency order - game then user."
  []
  (when-let [components (:components @system)]
    (when (:game components)
      (try
        (log/info "Stopping game engine scheduler...")
        (require '[cc.mindward.game.interface :as game])
        ((resolve 'cc.mindward.game.interface/stop-cleanup-scheduler!))
        (log/info "Game engine stopped.")
        (catch Exception e
          (log/error e "Error stopping game engine"))))
    
    ;; Note: User component uses SQLite (file-based), no connection pool to close
    (when (:user components)
      (log/info "User component shutdown (no cleanup needed for SQLite)."))))

(defn- init-components!
  "Initialize components with proper lifecycle management.
   ∃ (Truth): Returns component registry for tracking."
  []
  (log/info "Initializing components...")
  (require '[cc.mindward.user.interface :as user])
  (require '[cc.mindward.game.interface :as game])
  
  (log/info "  → User database...")
  ((resolve 'cc.mindward.user.interface/init!))
  
  (log/info "  → Game engine...")
  ((resolve 'cc.mindward.game.interface/initialize!))
  
  {:user :initialized
   :game :initialized})

(defn stop
  "Stop the running web server and shutdown all components.
   τ (Wisdom): Proper resource cleanup prevents leaks."
  []
  (when-let [sys @system]
    (when-let [server (:server sys)]
      (log/info "Stopping web server...")
      (try
        (.stop server)
        (log/info "Server stopped.")
        (catch Exception e
          (log/error e "Error stopping server"))))
    
    (when-let [watcher (:watcher sys)]
      (log/info "Stopping file watcher...")
      (stop-watcher! watcher)
      (log/info "File watcher stopped."))
    
    (shutdown-components!)
    
    (reset! system nil)
    (log/info "✅ System stopped cleanly.")))

(defn start
  "Start the web server with full component initialization.
   Server runs in background thread, REPL remains interactive.
   
   φ (Vitality): System runs independently while preserving REPL control."
  []
  (when @system
    (log/warn "System already running. Call (stop) first.")
    (throw (ex-info "System already running" {:system @system})))
  
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (log/info "Loading web server namespace...")
    (require '[cc.mindward.web-server.core :as web])
    (require '[ring.adapter.jetty :as jetty])
    
    ;; Initialize components (order matters - dependencies first)
    (let [components (init-components!)]
      
      (log/info "Starting Jetty web server on port" port "...")
      (let [run-jetty (resolve 'ring.adapter.jetty/run-jetty)
            site-app (resolve 'cc.mindward.web-server.core/site-app)
            server (run-jetty site-app {:port port :join? false})]
        
        ;; Store system state for lifecycle management
        (reset! system {:server server :components components})
        
        (log/info "")
        (log/info "═══════════════════════════════════════════════════════════")
        (log/info "  ✅ System started successfully")
        (log/info "     → http://localhost:" port)
        (log/info "     → Use (stop) to stop, (restart) for hot reload")
        (log/info "═══════════════════════════════════════════════════════════")
        (log/info "")
        
        server))))

(defn restart
  "Stop server, reload all changed namespaces, and restart server.
   This is the main hot reload function - use it after making code changes."
  []
  (log/info "Restarting with hot reload...")
  (stop)
  (tools-ns/refresh :after 'user/start))

(defn reset
  "Reload all changed namespaces without restarting the server.
   Use this for changes that don't require server restart (e.g., pure functions)."
  []
  (log/info "Reloading changed namespaces...")
  (tools-ns/refresh))

;; ------------------------------------------------------------
;; File Watcher Control (Advanced)
;; ------------------------------------------------------------

(defn watch-on
  "Enable automatic file watching and hot reload.
   Files will be checked every 2 seconds and auto-reloaded on changes."
  []
  (if-let [sys @system]
    (if (:watcher sys)
      (log/warn "File watcher already running.")
      (do
        (log/info "Starting file watcher...")
        (let [watcher (start-watcher!)]
          (swap! system assoc :watcher watcher)
          (log/info "✅ File watcher started. Changes will auto-reload every 2 seconds."))))
    (log/warn "System not running. Start system first with (start).")))

(defn watch-off
  "Disable automatic file watching.
   You'll need to manually call (restart) after code changes."
  []
  (if-let [watcher (:watcher @system)]
    (do
      (log/info "Stopping file watcher...")
      (stop-watcher! watcher)
      (swap! system dissoc :watcher)
      (log/info "✅ File watcher stopped. Use (restart) for manual reload."))
    (log/warn "File watcher not running.")))

;; ------------------------------------------------------------
;; Development Helpers
;; ------------------------------------------------------------

(defn status
  "Check if server is running and display comprehensive system status with health checks."
  []
  (if-let [sys @system]
    (do
      (println "\n═══════════════════════════════════════════════════════════")
      (println "  System Status")
      (println "═══════════════════════════════════════════════════════════")
      (println "✅ System is running")
      (println "   Server:" (if (:server sys) "Active" "Stopped"))
      (println "   Components:" (keys (:components sys)))
      (println "   File Watcher:" (if (:watcher sys) "🔍 Active (auto-reload enabled)" "⏸ Disabled (manual reload)"))
      (println "")
      
      ;; Component Health Checks
      (when ((:components sys) :game)
        (try
          (require '[cc.mindward.game.interface :as game])
          (let [health ((resolve 'cc.mindward.game.interface/health-check))]
            (println "📊 Game Engine:")
            (println "   Healthy:" (if (:healthy? health) "✅" "❌"))
            (println "   Scheduler:" (if (get-in health [:details :scheduler-running]) "Running" "Stopped"))
            (println "   Active Games:" (get-in health [:details :active-games]))
            (println "   Saved Games:" (get-in health [:details :saved-games])))
          (catch Exception e
            (println "📊 Game Engine: ❌ Health check failed -" (.getMessage e)))))
      
      (when ((:components sys) :user)
        (try
          (require '[cc.mindward.user.interface :as user])
          (let [health ((resolve 'cc.mindward.user.interface/health-check))]
            (println "")
            (println "💾 User Database:")
            (println "   Healthy:" (if (:healthy? health) "✅" "❌"))
            (println "   Connected:" (if (get-in health [:details :database-connected]) "Yes" "No"))
            (println "   User Count:" (get-in health [:details :user-count])))
          (catch Exception e
            (println "💾 User Database: ❌ Health check failed -" (.getMessage e)))))
      
      (println "═══════════════════════════════════════════════════════════\n"))
    (println "❌ System is stopped")))

(defn help
  "Display development environment help."
  []
  (println "
═══════════════════════════════════════════════════════════
  Simplicity Development Environment - Hot Reload Enabled
═══════════════════════════════════════════════════════════

Basic Commands:
  (start)     Start the web server (port 3000)
  (stop)      Stop the web server
  (restart)   Hot reload: stop, reload code, restart
  (reset)     Reload changed namespaces (no restart)
  (status)    Check system status with health checks
  (help)      Display this help

File Watcher (Auto-Reload):
  (watch-on)  Enable automatic file watching (checks every 2s)
  (watch-off) Disable automatic file watching

Workflow (Manual Reload):
  1. Start REPL: clojure -M:nrepl
  2. In REPL:   (start)
  3. Edit code in components/bases
  4. In REPL:   (restart)  ; ← Hot reload!
  5. Test changes at http://localhost:3000

Workflow (Auto-Reload):
  1. Start REPL: clojure -M:nrepl
  2. In REPL:   (start)
  3. In REPL:   (watch-on)   ; ← Enable auto-reload
  4. Edit code in components/bases
  5. Wait 2 seconds - changes reload automatically!
  6. Test changes at http://localhost:3000

Tips:
  - (status) shows health checks for all components
  - (restart) reloads ALL changed namespaces automatically
  - (watch-on) enables hands-free development
  - Server runs in background, REPL stays interactive
  - Use (reset) for lightweight changes (no server restart)
  - Check logs for errors during reload

Component Namespaces:
  cc.mindward.game.interface    - Game of Life engine
  cc.mindward.auth.interface    - User authentication
  cc.mindward.user.interface    - User management
  cc.mindward.ui.interface      - HTML rendering

Philosophy: 易简则天下之理得
  (Simplicity allows obtaining the logic of the world)
"))

;; ------------------------------------------------------------
;; Auto-print help on REPL startup
;; ------------------------------------------------------------

(println "\n✅ Development environment loaded.")
(println "   Type (help) for commands, (start) to begin.\n")

(comment
  ;; Development workflow examples
  
  ;; Start server
  (start)
  
  ;; Make code changes in components/bases...
  
  ;; Hot reload (most common)
  (restart)
  
  ;; Just reload code (no server restart)
  (reset)
  
  ;; Stop server
  (stop)
  
  ;; Check status
  (status)
  )
