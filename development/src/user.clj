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

   Philosophy (∃ Truth):
     Hot reload should preserve application state while reloading logic.
     Proper lifecycle management prevents resource leaks."
  (:require [clojure.tools.namespace.repl :as tools-ns]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]))

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
;; Structure: {:server <jetty> :components {:user ... :game ...}}

(defonce ^:private system (atom nil))

;; ------------------------------------------------------------
;; Auto-Reload File Watcher (φ Vitality)
;; ------------------------------------------------------------
;; Watch for .clj/.js file changes and auto-restart the system
;; Uses standard Java APIs for Clojure REPL compatibility

(defonce ^:private file-watcher (atom nil))
(defonce ^:private last-modified (atom 0))
(defonce ^:private refresh-future (atom nil))  ;; Track refresh future for cancellation

(def ^:private watched-dirs
  "Directories to watch for changes."
  ["components/auth/src"
   "components/user/src"
   "components/game/src"
   "components/ui/src"
   "bases/web-server/src"
   "bases/web-server/resources"])

(defn- get-last-modified
  "Get the most recent modification time across all source files.
   Uses standard Java File APIs for REPL compatibility."
  []
  (let [exts #{"clj" "cljs" "cljc" "js" "css"}]
    (reduce (fn [max-time dir]
              (try
                (let [dir-file (io/file dir)]
                  (if (.exists dir-file)
                    (reduce (fn [t f]
                              (let [fname (str f)
                                    ext (last (str/split fname #"\."))]
                                (if (and ext (exts ext))
                                  (max t (.lastModified f))
                                  t)))
                            max-time
                            (file-seq dir-file))
                    max-time))
                (catch Exception _ max-time)))
            0
            watched-dirs)))

(declare start stop)

(defn- watch-files!
  "Start background thread to watch for file changes and auto-restart.
   Uses future on REPL thread to avoid *ns* binding issues."
  []
  (when @file-watcher
    (.interrupt @file-watcher))
  (reset! file-watcher
          (future
            (loop []
              (Thread/sleep 1000)
              (let [current-mod (get-last-modified)]
                (when (and @system (> current-mod @last-modified))
                  (reset! last-modified current-mod)
                  (log/info "📁 File change detected, auto-reloading...")
                  (try
                    ;; Cancel any pending refresh
                    (when @refresh-future (future-cancel @refresh-future))
                    ;; Run refresh on REPL thread via future with proper binding
                    (reset! refresh-future
                            (future
                              (binding [*ns* (find-ns 'user)]
                                (stop)
                                (tools-ns/refresh)
                                (start))))
                    ;; Wait for refresh to complete (with timeout)
                    (when @refresh-future
                      (deref @refresh-future 30000 nil))
                    (log/info "✅ Auto-reload complete")
                    (catch Exception e
                      (log/error e "Auto-reload failed")))))
              (recur)))))

(defn- stop-file-watcher!
  "Stop the file watcher thread."
  []
  (when @refresh-future
    (future-cancel @refresh-future)
    (reset! refresh-future nil))
  (when @file-watcher
    (future-cancel @file-watcher)
    (reset! file-watcher nil)))

;; Toggle auto-reload on/off
(defonce ^:private auto-reload-enabled? (atom false))

(defn auto-reload
  "Enable or disable auto-reload on file changes.
   Usage: (auto-reload true)  ;; enable
          (auto-reload false) ;; disable"
  [enabled?]
  (reset! auto-reload-enabled? enabled?)
  (if enabled?
    (do
      (reset! last-modified (get-last-modified))
      (watch-files!)
      (log/info "👁️ Auto-reload enabled - watching for file changes"))
    (do
      (stop-file-watcher!)
      (log/info "👁️ Auto-reload disabled")))
  (println (if enabled? "✅ Auto-reload ON" "⏸️ Auto-reload OFF")))

;; ------------------------------------------------------------
;; Lifecycle Functions
;; ------------------------------------------------------------

(defn- shutdown-components!
  "Properly shutdown all initialized components.
   π (Synthesis): Reverse dependency order - game then user."
  []
  (when-let [components (:components @system)]
    (when (:game components)
      (try
        (log/info "Stopping game engine scheduler...")
        (when-not (contains? (ns-aliases 'user) 'game-iface)
          (require '[cc.mindward.game.interface :as game-iface]))
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
  ;; Remove existing aliases if they conflict
  (when (contains? (ns-aliases 'user) 'user-iface)
    (ns-unalias 'user 'user-iface))
  (when (contains? (ns-aliases 'user) 'game-iface)
    (ns-unalias 'user 'game-iface))
  (require '[cc.mindward.user.interface :as user-iface])
  (require '[cc.mindward.game.interface :as game-iface])

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
    ;; Stop file watcher first
    (stop-file-watcher!)

    (when-let [server (:server sys)]
      (log/info "Stopping web server...")
      (try
        (.stop server)
        (log/info "Server stopped.")
        (catch Exception e
          (log/error e "Error stopping server"))))

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
    ;; Remove existing aliases if they conflict
    (when (contains? (ns-aliases 'user) 'web)
      (ns-unalias 'user 'web))
    (when (contains? (ns-aliases 'user) 'jetty)
      (ns-unalias 'user 'jetty))
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

        ;; Enable auto-reload by default
        (auto-reload true)

        (log/info "")
        (log/info "═══════════════════════════════════════════════════════════")
        (log/info "  ✅ System started successfully")
        (log/info "     → http://localhost:" port)
        (log/info "     → Auto-reload enabled (watching for file changes)")
        (log/info "     → Use (stop) to stop, (restart) for manual hot reload")
        (log/info "═══════════════════════════════════════════════════════════")
        (log/info "")

        server))))

(defn restart
  "Stop server, reload all changed namespaces, and restart server.
   This is the main hot reload function - use it after making code changes."
  []
  (if @system
    (do
      (log/info "Restarting with hot reload...")
      (stop)
      ;; Refresh with proper namespace binding
      (binding [*ns* (find-ns 'user)]
        (tools-ns/refresh))
      (start))
    (do
      (println)
      (println "❌ Cannot restart - system not running!")
      (println "   Run (start) first to start the system.")
      (println))))

(defn reset
  "Reload all changed namespaces without restarting the server.
   Use this for changes that don't require server restart (e.g., pure functions)."
  []
  (log/info "Reloading changed namespaces...")
  (tools-ns/refresh))

;; ------------------------------------------------------------
;; Development Helpers
;; ------------------------------------------------------------

(defn banner
  "Display welcome banner with quick reference commands.
   (μ Directness): Show developers exactly what they need."
  []
  (println)
  (println "═══════════════════════════════════════════════════════════")
  (println "  易简则天下之理得")
  (println "  Simplicity - Development REPL")
  (println "═══════════════════════════════════════════════════════════")
  (println)
  (println "📋 QUICK START:")
  (println "   (start)      - Start web server (http://localhost:3000)")
  (println "   (stop)       - Stop server and cleanup")
  (println "   (restart)    - Manual hot reload")
  (println "   (status)     - Show system health and database stats")
  (println)
  (println "🔧 AUTO-RELOAD:")
  (println "   👁️ Auto-reload is ENABLED by default!")
  (println "   → Edit any .clj file and changes apply automatically")
  (println "   → Server restarts ~1 second after file change detected")
  (println "   → Use (auto-reload false) to disable")
  (println)
  (println "🧪 TESTING:")
  (println "   Shell: bb test        - Run all 618 tests")
  (println "   Shell: bb test:watch  - Watch mode")
  (println "   Shell: bb lint        - Lint all sources")
  (println)
  (println "📚 HELP:")
  (println "   Shell: bb help        - Show all bb tasks")
  (println "   (banner)              - Show this message again")
  (println "═══════════════════════════════════════════════════════════")
  (println)
  (println "💡 TIP: Run (start) to begin development")
  (println))

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
      (println "")

      ;; Component Health Checks
      (when ((:components sys) :game)
        (try
          (when-not (contains? (ns-aliases 'user) 'game-iface)
            (require '[cc.mindward.game.interface :as game-iface]))
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
          (when-not (contains? (ns-aliases 'user) 'user-iface)
            (require '[cc.mindward.user.interface :as user-iface]))
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

;; ------------------------------------------------------------
;; Auto-display banner on REPL startup
;; (φ Vitality): Welcome developers with helpful context
;; ------------------------------------------------------------

(banner)

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
  (status))