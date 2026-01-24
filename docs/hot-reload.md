# Hot Reload Development Workflow

> **Philosophy**: "易简则天下之理得" (Simplicity allows obtaining the logic of the world)

Instead of restarting the JVM every time you make a change, we keep it running and reload only the changed code. This reduces the feedback loop from ~30 seconds to **<1 second**.

---

## 🚀 Quick Start

### 1. Start Development Environment
```bash
bb dev
```

### 2. Start the Server
Once the REPL loads, type:
```clojure
user=> (start)
```
The server starts on http://localhost:3000.

### 3. Edit & Reload
Edit any `.clj` file, then in the REPL:
```clojure
user=> (restart)
```
**Total time: ~0.5s - 2s**.

---

## 🛠 Available Commands

| Command | Action | Use Case |
|---------|--------|----------|
| `(start)` | Start web server | First time startup |
| `(stop)` | Stop web server | Manual cleanup |
| **`(restart)`** | **Hot reload** | **Main workflow after any code change** |
| `(reset)` | Reload code only | Changes to pure functions (no restart) |
| `(status)` | System health | Check server & component state |
| `(help)` | Show help | Reference commands |

---

## 💡 Best Practices

### 1. State Preservation
Use `defonce` for stateful atoms (DB pools, game state, server instances). This prevents data loss during reloads.
```clojure
;; ✅ CORRECT: Preserves state
(defonce ^:private games (atom {}))
```

### 2. Lifecycle Management
Components should provide `init!` and `stop!` functions. The `user` namespace manages these in `(start)` and `(stop)`.
- **Start**: Initialize components → Start server.
- **Stop**: Stop server → Shutdown components (in reverse dependency order).

### 3. Path Hygiene
Only `src` directories are tracked for reloads. Test directories are excluded to prevent dependency conflicts and keep reloads fast.

### 4. Non-Blocking Server
The server runs with `:join? false`, keeping the REPL interactive at all times.

---

## 🔍 How It Works

We use `clojure.tools.namespace.repl` to:
1. Scan for changed `.clj` files.
2. Unload namespaces in reverse dependency order.
3. Reload namespaces in dependency order.
4. Restart the server with the new code.

**Performance**: 
- Cold Start: ~30s
- Hot Reload: **~0.5s** (60x speedup)

---

## 🆘 Troubleshooting

- **"Server already running"**: Call `(stop)` first or just use `(restart)`.
- **Reload not picking up changes**: Ensure the file is saved and in a tracked `src` directory.
- **Syntax Error**: Fix the error in your editor and call `(restart)` again.
- **Confused State**: Call `(clojure.tools.namespace.repl/clear)` then `(restart)`.

---
*Last updated: 2024-05-20*
