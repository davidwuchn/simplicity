# Clojure-MCP Quick Start Guide

## ⚡ TL;DR - Get Started in 30 Seconds

### Claude Desktop
```bash
# Terminal: Start development environment with hot reload
bb dev

# In REPL:
user=> (start)     ; Start server

# Then restart Claude Desktop and add "Project Summary" resource
```

**Workflow**: AI edits code → You run `(restart)` in REPL → Test (1-2 sec!)

### Claude Code (CLI)
```bash
# One-time setup
claude mcp add clojure-mcp -- clojure -Tmcp start \
  :start-nrepl-cmd '["clojure" "-M:nrepl"]'

# Start session
claude

# In Claude session, start server
> (start)
```

**Workflow**: AI edits code → Ask AI to run `(restart)` → Test (1-2 sec!)

---

## 📋 Component Namespaces

All components are now loaded automatically when nREPL starts.

```clojure
;; Game of Life engine
(require '[cc.mindward.game.interface :as game])

;; User authentication
(require '[cc.mindward.auth.interface :as auth])

;; User management & database
(require '[cc.mindward.user.interface :as user])

;; HTML rendering (Hiccup)
(require '[cc.mindward.ui.interface :as ui])
```

**Note**: Namespaces are `cc.mindward.<name>`, **not** `cc.mindward.component.<name>`

---

## 🔧 Common Commands

### Development Workflow (Hot Reload)
```bash
# Start development environment
bb dev

# In REPL:
user=> (start)     # Start web server (port 3000)
user=> (restart)   # Hot reload after code changes (1-2 sec!)
user=> (stop)      # Stop server
user=> (status)    # Check status
user=> (help)      # Show all commands
```

**Main workflow**: Edit code → `(restart)` → Test in browser

### Check if nREPL is Running
```bash
lsof -i :7888
cat .nrepl-port
```

### Start nREPL Manually (if needed)
```bash
# With hot reload environment (recommended)
bb dev

# Or minimal nREPL
clojure -M:nrepl
```

### Stop nREPL
```bash
pkill -f "clojure.*nrepl"
```

### Test REPL Connection
```bash
brepl <<'EOF'
(require '[cc.mindward.game.interface :as game])
(println "✅ Connected!")
EOF
```

### Hot Reload Workflow
```clojure
;; In REPL after starting with bb dev
user=> (start)           ; Start server
;; Edit code...
user=> (restart)         ; Hot reload (1-2 seconds!)
;; Test at http://localhost:3000
```

### Verify Polylith Workspace
```bash
clojure -M:poly check
clojure -M:poly info
```

---

## 🤖 What AI Assistants Can Do

### Code Evaluation
```
AI: "Let me test that function in the REPL..."
(require '[cc.mindward.game.interface :as game])
(game/create-game! :test #{[1 1] [1 2] [1 3]})
```

### Hot Reload Workflow
```
AI: "I've updated the game component. Let me reload..."
(restart)
;; INFO  Restarting with hot reload...
;; :reloading (cc.mindward.game.interface ...)
;; INFO  ✅ Server started
```

### File Operations
- Read project files
- Edit Clojure code (structure-aware)
- Create new namespaces
- Run tests

### Architectural Guidance
- Enforces Polylith constraints
- Follows Eight Keys philosophy (φ, fractal, e, τ, π, μ, ∃, ∀)
- Maintains security standards (501 test assertions)

### Forbidden Actions
- Cross-component `impl` dependencies
- Business logic in bases
- Hardcoded secrets
- Global mutable state

---

## 📚 Context Files (Auto-Loaded by MCP)

1. **PROJECT_SUMMARY.md** - Architecture overview, API reference
2. **LLM_CODE_STYLE.md** - Coding standards, Polylith rules
3. **AGENTS.md** - Operational guidelines, Eight Keys philosophy

These files give AI assistants full understanding of your codebase.

---

## 🔍 Troubleshooting

### "Could not locate namespace on classpath"
```bash
# nREPL wasn't started with component paths
# Restart with hot reload environment:
pkill -f "clojure.*nrepl"
bb dev
user=> (start)
```

### "Port 7888 already in use"
```bash
# Find and kill existing process
lsof -i :7888
pkill -f "clojure.*nrepl"
```

### "Syntax error in user.clj"
```bash
# Check for balanced parentheses
brepl balance development/src/user.clj
```

### Components Won't Load
```bash
# Verify paths are correct
clojure -M:nrepl -e "(println (System/getProperty \"java.class.path\"))"

# Should include: components/game/src, components/auth/src, etc.
```

---

## 🎯 Example AI Session

**You**: "Add a function to count live cells in the game board"

**AI**:
1. Reads `components/game/src/cc/mindward/game/interface.clj`
2. Uses `clojure_edit` to add function (structure-aware)
3. Uses `clojure_eval` to test: `(game/count-live-cells test-board)`
4. Uses `clojure_eval` to hot reload: `(restart)`

**Result**: Function added and tested in ~5 seconds!

**You**: "Great! Show me how to use it in the browser"

**AI**:
```clojure
;; Navigate to http://localhost:3000/game
;; Click cells to create pattern
;; Press Space to evolve
;; Score shown includes live cell count
```

---

## 🔐 Security Notes

- **Allowed Directories**: components, bases, projects, docs, scripts, etc.
- **Write Guards**: Partial read required before overwriting files
- **Max File Size**: 10MB
- **Timeouts**: 30s REPL, 60s bash
- **Confirmation Required**: Bash commands (configurable)

See `.clojure-mcp/config.edn` for full configuration.

---

## 📖 Resources

- **Integration Guide**: `docs/clojure-mcp-integration.md`
- **Status Report**: `.clojure-mcp/INTEGRATION_STATUS.md`
- **Clojure-MCP Docs**: https://github.com/bhauman/clojure-mcp
- **Polylith Docs**: https://polylith.gitbook.io/

---

**Status**: ✅ Verified working (2026-01-23)
