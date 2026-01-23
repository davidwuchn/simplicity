# Hot Reload Development Workflow

This guide explains how to develop with **instant hot reload** - no JVM restarts needed!

## Philosophy

**易简则天下之理得** (Simplicity allows obtaining the logic of the world)

Instead of restarting the JVM every time you make a change, we keep it running and reload only the changed code. This reduces the feedback loop from ~30 seconds to ~1 second.

---

## Quick Start

### 1. Start Development Environment

**Using Babashka (recommended):**

```bash
bb dev
```

**Without Babashka:**

```bash
clojure -M:nrepl
```

**Why Babashka?**
- ✅ Fast startup (milliseconds vs seconds)
- ✅ Unified command interface (`bb dev`, `bb test`, `bb build`)
- ✅ 30+ development tasks available
- ✅ Cross-platform compatibility

See `bb help` for all available commands.

### 2. Start the Server

Once the REPL loads, type:

```clojure
(start)
```

The web server will start on http://localhost:3000

### 3. Make Code Changes

Edit any file in:
- `components/*/src/` - Business logic
- `bases/web-server/src/` - HTTP handlers and routing
- `development/src/` - Dev tools

### 4. Hot Reload

In the REPL, type:

```clojure
(restart)
```

This will:
1. Stop the web server
2. Reload all changed namespaces
3. Restart the web server

**Total time: ~1-2 seconds** (vs ~30 seconds for full JVM restart)

---

## Available Commands

### `(start)`
Start the web server on port 3000 (default, or `$PORT`).

Server runs in a background thread, so your REPL stays interactive.

```clojure
user=> (start)
;; INFO  Initializing database...
;; INFO  Initializing game engine...
;; INFO  Starting web server on port 3000 ...
;; INFO  ✅ Server started on http://localhost: 3000
```

### `(stop)`
Stop the running web server.

```clojure
user=> (stop)
;; INFO  Stopping web server...
;; INFO  Server stopped.
```

### `(restart)`
**The main hot reload command!**

Stop server → Reload changed code → Restart server.

```clojure
user=> (restart)
;; INFO  Restarting with hot reload...
;; INFO  Stopping web server...
;; INFO  Server stopped.
;; :reloading (cc.mindward.game.interface cc.mindward.game.impl ...)
;; INFO  Starting web server on port 3000 ...
;; INFO  ✅ Server started on http://localhost: 3000
```

### `(reset)`
Reload changed namespaces **without** restarting the server.

Use this for changes to pure functions that don't affect server state.

```clojure
user=> (reset)
;; INFO  Reloading changed namespaces...
;; :reloading (cc.mindward.game.impl)
;; :ok
```

### `(status)`
Check if the server is running.

```clojure
user=> (status)
;; ✅ Server is running
```

### `(help)`
Display all available commands and workflow tips.

---

## Development Workflow Examples

### Basic Workflow

```clojure
;; 1. Start REPL
;; $ bb dev

;; 2. Start server
user=> (start)

;; 3. Open http://localhost:3000 in browser

;; 4. Edit components/game/src/cc/mindward/game/impl.clj
;;    (add a new function, fix a bug, etc.)

;; 5. Hot reload
user=> (restart)

;; 6. Refresh browser - changes are live!
```

### Testing Changes in REPL

```clojure
;; Start server
user=> (start)

;; Test a component function
user=> (require '[cc.mindward.game.interface :as game])
user=> (game/create-game! :test-game #{[1 1] [1 2] [1 3]})
user=> (game/evolve! :test-game)

;; Make changes to the component...

;; Reload just that component
user=> (require '[cc.mindward.game.interface :as game] :reload-all)

;; Test again
user=> (game/evolve! :test-game)

;; When satisfied, restart server to apply changes
user=> (restart)
```

### Fixing a Bug

```clojure
;; Server is running, you notice a bug

;; 1. Stop server to avoid conflicts
user=> (stop)

;; 2. Fix the bug in components/auth/src/cc/mindward/auth/impl.clj

;; 3. Reload the namespace
user=> (require '[cc.mindward.auth.impl :as auth-impl] :reload-all)

;; 4. Test the fix
user=> (auth-impl/hash-password "test123")

;; 5. Restart server with fix
user=> (start)
```

---

## How It Works

### tools.namespace Magic

The `clojure.tools.namespace.repl` library tracks:
- Which files have changed on disk
- The dependency graph between namespaces
- How to safely unload and reload code

When you call `(restart)`, it:
1. Scans the classpath for changed `.clj` files
2. Topologically sorts namespaces by dependencies
3. Unloads changed namespaces (in reverse dependency order)
4. Reloads changed namespaces (in dependency order)
5. Calls `user/start` to restart the server

### Server State Management

The `user` namespace maintains an atom with the Jetty server instance:

```clojure
(defonce ^:private server-instance (atom nil))
```

- `(start)` creates a new server and stores it in the atom
- `(stop)` calls `.stop` on the server and clears the atom
- `(restart)` combines stop + reload + start

The server runs with `:join? false`, so it doesn't block the REPL thread.

---

## Advanced Usage

### Environment Variables

Set custom port:
```bash
PORT=8080 bb dev
```

Then in REPL:
```clojure
user=> (start)
;; Server starts on port 8080
```

### Multiple REPLs

You can have multiple REPL sessions connected to the same nREPL server:

**Terminal 1:**
```bash
bb dev
```

**Terminal 2:**
```bash
clojure -M:nrepl -e "(require '[clojure.tools.nrepl :as nrepl])" \
  -e "(with-open [conn (nrepl/connect :port 7888)] \
        (-> (nrepl/client conn 1000) \
            (nrepl/message {:op :clone}) \
            nrepl/response-values))"
```

Or use your editor's nREPL client (e.g., CIDER, Calva, Cursive).

### Debugging Failed Reloads

If `(restart)` fails with an error:

```clojure
;; See what failed
user=> *e

;; Check the exception data
user=> (ex-data *e)

;; Manually reload the problematic namespace
user=> (require '[cc.mindward.problematic.namespace] :reload-all)

;; Fix the issue, then try again
user=> (restart)
```

### Resetting Namespace Tracker

If reload gets confused (rare), reset it:

```clojure
user=> (clojure.tools.namespace.repl/clear)
user=> (clojure.tools.namespace.repl/refresh-all)
```

---

## Limitations & Workarounds

### 1. Defonce Won't Reload

**Problem**: `(defonce state (atom {}))` won't reset on reload.

**Workaround**: Use `(reset! state initial-value)` in the REPL.

### 2. Protocol/Deftype Changes

**Problem**: Changing protocols or deftype/defrecord requires namespace consumers to reload.

**Workaround**: Use `(restart)` which reloads dependents automatically.

### 3. Macro Changes

**Problem**: Macros are expanded at compile-time, so changing a macro requires reloading consumers.

**Workaround**: `(clojure.tools.namespace.repl/refresh-all)` reloads everything.

### 4. Java Interop Changes

**Problem**: Can't reload Java classes without restarting JVM.

**Workaround**: Minimize Java interop in hot-reloadable code. Put it in a separate namespace.

---

## Troubleshooting

### "Server already running" Error

```clojure
user=> (start)
;; ERROR Server already running
```

**Fix**: Call `(stop)` first, or use `(restart)`.

### Port Already in Use

```bash
# Find process using port 3000
lsof -i :3000

# Kill it
kill -9 <PID>
```

### Reload Doesn't Pick Up Changes

**Possible causes**:
1. File not saved to disk → Save file
2. Syntax error in file → Check REPL for error messages
3. Namespace not on classpath → Verify file is in `components/*/src` or `bases/*/src`

**Debug**:
```clojure
;; Force refresh all
user=> (clojure.tools.namespace.repl/refresh-all)

;; Check what's being tracked
user=> (clojure.tools.namespace.dir/scan-dirs 
         (clojure.tools.namespace.repl/scan-dirs))
```

### Database Lock Errors

If you see "database is locked" errors:

```clojure
;; Stop server to release lock
user=> (stop)

;; If needed, reinitialize database
user=> (require '[cc.mindward.user.interface :as user])
user=> (user/init!)

;; Restart
user=> (start)
```

---

## Integration with clojure-mcp

The hot reload workflow works seamlessly with clojure-mcp:

### Claude Desktop / Claude Code

When an AI assistant makes code changes via clojure-mcp:

1. AI edits file using `clojure_edit` tool
2. You run `(restart)` in your local REPL
3. Changes are live instantly

**Example session:**

```
You: "Add a function to count live cells in the game component"

AI: [Uses clojure_edit to add function]

You: [In REPL] (restart)
     ✅ Hot reload complete!

You: "Test it on the /game page"

AI: [Uses clojure_eval to test the new function]
```

### Automated Workflow

You can even automate the reload from clojure-mcp:

```clojure
;; In .clojure-mcp/config.edn, add a custom command
:custom-commands
{:reload-server
 {:description "Hot reload the web server"
  :command "(do (require 'user :reload) (user/restart))"}}
```

Then AI can trigger reload after making changes.

---

## Performance Tips

### 1. Faster Reloads

Only reload what changed:
```clojure
;; Instead of (restart) for tiny changes
user=> (reset)  ; Just reload, don't restart server
```

### 2. Parallel Development

Keep server running, test components in REPL:
```clojure
user=> (start)

;; In parallel, test individual functions
user=> (require '[cc.mindward.game.interface :as game] :reload)
user=> (game/evolve! :test-game)

;; When satisfied, restart to apply
user=> (restart)
```

### 3. Selective Reloading

For large codebases, reload only specific namespaces:
```clojure
user=> (require '[cc.mindward.game.impl] :reload-all)
user=> (restart)
```

---

## Comparison: Before vs After

### Before (Manual JVM Restart)

1. Edit code
2. `Ctrl-C` to stop server
3. Wait for JVM shutdown (~5s)
4. `clojure -M -m cc.mindward.web-server.core`
5. Wait for JVM startup (~15s)
6. Wait for server initialization (~10s)
7. **Total: ~30 seconds**

### After (Hot Reload)

1. Edit code
2. `(restart)` in REPL
3. **Total: ~1-2 seconds**

**Result**: 15-30x faster feedback loop! 🚀

---

## Further Reading

- **Polylith REPL Workflow**: https://polylith.gitbook.io/polylith/workflow/development
- **tools.namespace**: https://github.com/clojure/tools.namespace
- **REPL-Driven Development**: https://clojure.org/guides/repl/introduction

---

## Quick Reference

| Command | Action | Use Case |
|---------|--------|----------|
| `(start)` | Start server | First time, or after `(stop)` |
| `(stop)` | Stop server | Debugging, manual testing |
| `(restart)` | Hot reload | **Main workflow** - after code changes |
| `(reset)` | Reload code only | Pure function changes (no restart) |
| `(status)` | Check server | Verify running state |
| `(help)` | Show commands | Reminder of available functions |

**Most common**: Edit code → `(restart)` → Test in browser

---

**Status**: ✅ Hot reload workflow active and verified  
**Philosophy**: 易简则天下之理得 (Simplicity allows obtaining the logic of the world)
