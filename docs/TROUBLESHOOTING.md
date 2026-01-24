# Troubleshooting Guide

This guide helps you diagnose and resolve common issues when working with Simplicity.

---

## 🚀 Quick Diagnostics

Run these commands first to check the state of the workspace:

```bash
bb check           # Check Polylith architecture integrity
bb lint            # Check for code style and syntax issues
bb test            # Run full test suite (652 assertions)
```

---

## 🛠 Development Environment

### REPL Won't Start
**Symptoms**: `bb dev` fails or connection refused on port 7888.

**Solutions**:
1. **Check for existing REPL**: `lsof -i :7888` and kill the process if needed.
2. **Java Version**: Ensure you are using Java 17+ (`java -version`).
3. **Memory**: If out of memory, increase heap: `JAVA_OPTS="-Xmx1g" bb dev`.

### Hot Reload Not Working
**Symptoms**: Changes to `.clj` files don't reflect after `(restart)`.

**Solutions**:
1. **Save Files**: Ensure your editor has actually saved the changes to disk.
2. **Syntax Errors**: A syntax error in any tracked file can block the refresh. Check the REPL output for "Syntax error".
3. **Manual Reset**: If the tracker gets confused, run `(clojure.tools.namespace.repl/clear)` then `(restart)`.

### "Server already running" Error
**Symptoms**: `(start)` fails with "System already running".

**Solution**: Use `(restart)` to stop and start the server in one go, or call `(stop)` before `(start)`.

---

## 🧱 Architecture (Polylith)

### Polylith Check Fails
**Symptoms**: `bb check` returns errors about circular dependencies or illegal imports.

**Solutions**:
1. **Impl to Impl**: Ensure you are not requiring an `impl` namespace from another component. Only require `interface` namespaces.
2. **Circular Deps**: Component A depends on B, and B depends on A. Extract shared logic into a third component.
3. **Missing Interface**: Ensure the function you're calling is actually defined in the component's `interface` namespace.

---

## 💾 Database & State

### Database Locked (SQLite)
**Symptoms**: `org.sqlite.SQLiteException: [SQLITE_BUSY] The database file is locked`.

**Solutions**:
1. **Stop Server**: Call `(stop)` in the REPL to release the database connection.
2. **Zombie Processes**: `lsof simplicity.db` to find and kill processes holding the file open.
3. **Cleanup**: Delete `simplicity.db-journal` if it exists.

### State Resetting on Reload
**Symptoms**: Game data or user sessions disappear after `(restart)`.

**Solution**: Ensure stateful atoms are defined with `defonce` instead of `def`.
```clojure
(defonce my-state (atom {})) ;; ✅ Good
(def my-state (atom {}))     ;; ❌ Bad (resets on reload)
```

---

## 🌐 Web Server & API

### Port 3000 Already in Use
**Solution**: 
- Kill the process: `lsof -ti:3000 | xargs kill -9`
- Or use a different port: `PORT=3001 bb dev`

### CSRF Token Errors
**Symptoms**: `403 Forbidden - Invalid anti-forgery token` on POST requests.

**Solutions**:
1. **Missing Header**: Ensure your `fetch` call includes the `x-csrf-token` header.
2. **Session Mismatch**: If you restarted the server, you might need to refresh the page to get a new token.

---

## 🧪 Testing

### Tests Failing Locally
**Solutions**:
1. **Clean Workspace**: `bb clean` to remove old artifacts.
2. **Isolated Tests**: Run only the failing brick to isolate the issue: `bb test:game`.
3. **Verify Integrity**: Run `bb check` to ensure there are no architectural violations causing weird test behavior.

---

## 📖 Getting Help

1. **REPL Help**: Run `(help)` or `(banner)` in the REPL.
2. **Documentation**: Check the `docs/` directory, especially `INDEX.md`.
3. **Philosophy**: Review `AGENTS.md` to ensure you're following the core principles.

---
*Last Updated: 2024-05-20*
