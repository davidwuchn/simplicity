# Troubleshooting Guide

This guide helps you diagnose and resolve common issues when working with Simplicity.

## Table of Contents
- [Installation Issues](#installation-issues)
- [Development Environment](#development-environment)
- [Database Issues](#database-issues)
- [Web Server Issues](#web-server-issues)
- [Docker Issues](#docker-issues)
- [Performance Issues](#performance-issues)
- [Security Issues](#security-issues)
- [Build Issues](#build-issues)
- [Testing Issues](#testing-issues)

---

## Installation Issues

### Java Version Mismatch

**Problem**: Error about incompatible Java version
```
Unsupported class file major version XX
```

**Solution**:
```bash
# Check your Java version
java -version

# Should be Java 17 or higher
# Install Java 17+ if needed (Ubuntu/Debian)
sudo apt install openjdk-17-jdk

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Clojure CLI Not Found

**Problem**: `clojure: command not found`

**Solution**:
```bash
# Install Clojure CLI (Linux/macOS)
curl -O https://download.clojure.org/install/linux-install-1.11.1.1413.sh
chmod +x linux-install-1.11.1.1413.sh
sudo ./linux-install-1.11.1.1413.sh

# Verify installation
clojure --version
```

### Polylith Tool Not Working

**Problem**: `poly: command not found` or Polylith errors

**Solution**:
```bash
# Polylith is installed via deps.edn alias, not globally
# Always use the :poly alias
clojure -M:poly info

# If still issues, verify deps.edn has :poly alias
grep -A 5 ":poly" deps.edn
```

---

## Development Environment

### REPL Won't Start

**Problem**: REPL fails to start with connection errors

**Solution 1**: Check if port is already in use
```bash
# Check if port 7888 (nREPL default) is in use
lsof -i :7888
# or
netstat -tuln | grep 7888

# Kill the process if needed
kill -9 <PID>
```

**Solution 2**: Start REPL with explicit port
```bash
clojure -A:dev:poly -J-Dclojure.server.repl="{:port 7888 :accept clojure.core.server/repl}"
```

**Solution 3**: Check for syntax errors
```bash
# Use brepl to check file balance
brepl balance components/game/src/cc/mindward/component/game/impl.clj --dry-run
```

### Syntax Errors After Editing

**Problem**: `Syntax error compiling at (file.clj:X:Y)`

**Solution**:
```bash
# ALWAYS run brepl balance after editing Clojure files
brepl balance <file-path>

# Check all files in a directory
find components -name "*.clj" -exec brepl balance {} \;

# Common issues:
# - Unbalanced parentheses
# - Missing closing brackets
# - Stray characters
```

### Namespace Not Found

**Problem**: `Could not locate cc/mindward/component/example/interface__init.class`

**Solution 1**: Verify namespace matches file path
```clojure
;; File: components/example/src/cc/mindward/component/example/interface.clj
;; Namespace MUST be:
(ns cc.mindward.component.example.interface)
```

**Solution 2**: Restart REPL after structural changes
```bash
# Exit current REPL (Ctrl+D or Ctrl+C)
# Start fresh
clojure -A:dev:poly
```

**Solution 3**: Check Polylith workspace
```bash
# Ensure component is registered
clojure -M:poly info

# Check for errors
clojure -M:poly check
```

### Circular Dependency Error

**Problem**: `Cyclic load dependency: [ component.a -> component.b -> component.a ]`

**Solution**: Violates Polylith architecture
```bash
# Check dependencies
clojure -M:poly check

# Fix: Components should never depend on each other circularly
# Refactor to extract shared logic into a new component
# or use data passing instead of direct calls
```

---

## Database Issues

### Database Locked

**Problem**: `database is locked` error

**Solution**:
```bash
# SQLite allows only one writer at a time
# Check for other processes accessing the database
lsof | grep simplicity.db

# Stop the web server
# Delete the lock file if it exists
rm simplicity.db-journal

# Restart the application
```

### Database File Not Found

**Problem**: Application creates new database on each start

**Solution**: Check DB_PATH environment variable
```bash
# Set persistent database path
export DB_PATH=/path/to/persistent/simplicity.db

# For Docker, use volume mount
docker run -v ./data:/app/data -e DB_PATH=/app/data/simplicity.db ...
```

### Schema Migration Failed

**Problem**: Old database schema, missing columns

**Solution**:
```bash
# Backup existing database
cp simplicity.db simplicity.db.backup

# Delete and recreate (loses data)
rm simplicity.db
# Restart application to create fresh schema

# OR manually migrate (see components/user/src/.../impl.clj for schema)
sqlite3 simplicity.db
sqlite> ALTER TABLE users ADD COLUMN new_field TEXT;
```

---

## Web Server Issues

### Port Already in Use

**Problem**: `Address already in use (Bind failed)`

**Solution**:
```bash
# Check what's using port 3000
lsof -i :3000

# Kill the process
kill -9 <PID>

# Or use a different port
export PORT=8080
```

### 404 Not Found on All Routes

**Problem**: All routes return 404

**Solution 1**: Check if server is running
```bash
curl http://localhost:3000/health
# Should return: {"status":"ok"}
```

**Solution 2**: Check route configuration
```clojure
;; In bases/web-server/src/cc/mindward/base/web_server/core.clj
;; Verify routes are registered in app-routes
```

**Solution 3**: Check for middleware issues
```bash
# Check logs for middleware errors
tail -f logs/app.log
```

### CSRF Token Error

**Problem**: `403 Forbidden - Invalid anti-forgery token`

**Solution 1**: Include CSRF token in forms
```html
<!-- In HTML forms -->
<input type="hidden" name="__anti-forgery-token" value="{{csrf-token}}">
```

**Solution 2**: Include CSRF token in fetch requests
```javascript
// In JavaScript fetch calls
fetch('/api/endpoint', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'x-csrf-token': document.querySelector('meta[name="csrf-token"]').content
  }
})
```

**Solution 3**: Disable CSRF for testing (NOT PRODUCTION)
```clojure
;; In web-server core, temporarily comment out csrf middleware
;; ONLY FOR LOCAL TESTING
```

### Rate Limiting Blocking Requests

**Problem**: `429 Too Many Requests`

**Solution**: Wait for rate limit to reset
```bash
# Login: 5 attempts per minute per IP
# Signup: 3 attempts per minute per IP

# Wait 60 seconds and retry
# For development, you can modify rate limits in:
# bases/web-server/src/cc/mindward/base/web_server/security.clj
```

---

## Docker Issues

### Docker Build Fails

**Problem**: `ERROR [builder X/Y] ...`

**Solution 1**: Check Java version in Dockerfile
```dockerfile
# Ensure using Java 17+
FROM eclipse-temurin:17-jdk-alpine AS builder
```

**Solution 2**: Clear Docker cache
```bash
docker builder prune -a -f
docker build --no-cache -t simplicity:latest .
```

**Solution 3**: Check available disk space
```bash
df -h
docker system df
docker system prune -a
```

### Container Exits Immediately

**Problem**: `docker run` exits with code 1

**Solution**:
```bash
# Check container logs
docker logs <container-id>

# Common issues:
# 1. Missing environment variables
docker run -e PORT=3000 -e DB_PATH=/app/data/simplicity.db ...

# 2. Permission issues (volume mounts)
docker run -v $(pwd)/data:/app/data --user $(id -u):$(id -g) ...

# 3. Port conflicts
docker run -p 8080:3000 ...  # Map to different host port
```

### Volume Mount Not Persisting Data

**Problem**: Database resets on container restart

**Solution**:
```bash
# Create named volume
docker volume create simplicity-data

# Use named volume
docker run -v simplicity-data:/app/data ...

# Or use absolute host path
docker run -v /absolute/path/to/data:/app/data ...

# Check volume contents
docker volume inspect simplicity-data
```

---

## Performance Issues

### Slow Game Evolution

**Problem**: Game of Life updates are slow

**Solution 1**: Check browser performance
```javascript
// Open DevTools Console
// Check for errors or warnings
console.log(performance.now())
```

**Solution 2**: Reduce grid size
```javascript
// In client code, reduce GRID_SIZE
// Default is likely 50x50 (2500 cells)
// Try 30x30 (900 cells) for better performance
```

**Solution 3**: Profile the game loop
```clojure
;; In components/game/src/.../impl.clj
;; Add timing around evolve-cells
(time (evolve-cells grid))
```

### High Memory Usage

**Problem**: Application consumes excessive memory

**Solution 1**: Adjust JVM heap size
```bash
# For uberjar
java -Xmx512m -Xms256m -jar target/simplicity-standalone.jar

# For Docker
docker run -e JAVA_OPTS="-Xmx512m -Xms256m" ...
```

**Solution 2**: Check for memory leaks
```bash
# Use JVM monitoring tools
jstat -gc <pid> 1000  # GC stats every second
jmap -heap <pid>       # Heap summary
```

**Solution 3**: Reduce session storage
```clojure
;; In web-server config, reduce session timeout
:session {:cookie-attrs {:max-age 3600}}  ; 1 hour instead of default
```

### Slow Database Queries

**Problem**: Database operations are slow

**Solution 1**: Add indexes (if missing)
```sql
-- For user lookups
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_scores_user_id ON scores(user_id);
```

**Solution 2**: Use connection pooling
```clojure
;; In user component, configure HikariCP
;; (Currently using direct connections)
```

**Solution 3**: Check query plans
```bash
sqlite3 simplicity.db
sqlite> EXPLAIN QUERY PLAN SELECT * FROM users WHERE username = ?;
```

---

## Security Issues

### HTTPS Redirect Loop

**Problem**: Infinite redirect when ENABLE_HSTS=true without HTTPS

**Solution**:
```bash
# ONLY enable HSTS when using HTTPS
# If behind Cloudflare or reverse proxy with HTTPS:
export ENABLE_HSTS=true

# If using HTTP locally:
export ENABLE_HSTS=false
```

### Session Not Persisting

**Problem**: User logged out on each request

**Solution 1**: Check session cookie configuration
```clojure
;; In web-server middleware
;; Ensure :session middleware is included
;; Check cookie-attrs (secure, http-only, same-site)
```

**Solution 2**: Check browser cookie settings
```bash
# In browser DevTools > Application > Cookies
# Verify "ring-session" cookie exists
# Check expiration, domain, path
```

### Password Validation Failing

**Problem**: Valid passwords rejected

**Solution**: Check password requirements
```clojure
;; Current requirements (in auth component):
;; - Minimum 8 characters
;; - No maximum (within reason)

;; Common issues:
;; - Whitespace (trimmed automatically)
;; - Special characters (all allowed)
```

---

## Build Issues

### Uberjar Build Fails

**Problem**: `clojure -T:build uberjar` fails

**Solution 1**: Clean target directory
```bash
rm -rf target
clojure -T:build uberjar
```

**Solution 2**: Check for compilation errors
```bash
# Ensure all tests pass first
clojure -M:poly test :dev

# Check for AOT compilation issues
clojure -M:poly check
```

**Solution 3**: Verify build.clj
```bash
# Check build configuration
cat build.clj

# Ensure basis is correct
clojure -T:build basis
```

### AOT Compilation Warnings

**Problem**: Warnings during uberjar build

**Solution**: Most warnings are safe to ignore
```bash
# Common warnings:
# "already refers to: #'clojure.core/..." - Override warnings (safe)
# "reflection warning" - Performance impact only

# To eliminate reflection warnings, add type hints
# (See Clojure documentation on type hints)
```

### Jar File Too Large

**Problem**: Uberjar is unexpectedly large (>100MB)

**Solution**:
```bash
# Current size should be ~45MB
# If larger, check for:

# 1. Duplicate dependencies
clojure -Stree | grep -i <lib-name>

# 2. Unnecessary dependencies in :prod alias
# Review deps.edn :prod alias

# 3. Test files included
# Ensure build.clj excludes test paths
```

---

## Testing Issues

### Tests Failing Locally

**Problem**: Tests pass in CI but fail locally

**Solution 1**: Ensure clean state
```bash
# Delete test databases
rm -f test-*.db

# Clear any cached state
rm -rf .cpcache

# Rerun tests
clojure -M:poly test :dev
```

**Solution 2**: Check Java version consistency
```bash
# CI uses Java 17 and 21
# Ensure local version matches
java -version
```

**Solution 3**: Check for test isolation issues
```clojure
;; Ensure tests use fixtures for cleanup
(use-fixtures :each
  (fn [f]
    (let [db (create-test-db)]
      (try
        (f)
        (finally
          (delete-test-db db))))))
```

### Test Database Errors

**Problem**: `database locked` or `table not found` in tests

**Solution**: Use unique test databases
```clojure
;; In test fixtures
(defn create-test-db []
  (let [db-path (str "test-" (java.util.UUID/randomUUID) ".db")]
    (initialize-db db-path)
    db-path))
```

### Slow Test Suite

**Problem**: Tests take too long (>60 seconds)

**Solution**:
```bash
# Run specific component tests
clojure -M:poly test brick:game

# Run tests in parallel (if supported)
# Currently runs sequentially

# Profile slow tests
# Add (time ...) around test bodies
```

---

## Getting Help

If you encounter issues not covered here:

1. **Check Logs**: `tail -f logs/app.log`
2. **Search Issues**: https://github.com/davidwuchn/simplicity/issues
3. **Create Issue**: Use bug report template
4. **Review Documentation**:
   - [README.md](../README.md)
   - [Architecture](architecture.md)
   - [Security](security.md)
   - [Deployment](deployment-cloudflare.md)
5. **Ask Questions**: Use question issue template

---

## Common Diagnostic Commands

```bash
# Check Polylith workspace health
clojure -M:poly check

# View workspace structure
clojure -M:poly info

# Run all tests
clojure -M:poly test :dev

# Check for outdated dependencies
clojure -M:outdated

# View dependency tree
clojure -Stree

# Check disk space
df -h

# Check memory usage
free -h

# Check running processes
ps aux | grep java

# Check port usage
lsof -i :3000

# Check Docker resources
docker system df
docker stats

# Verify database integrity
sqlite3 simplicity.db "PRAGMA integrity_check;"
```

---

## Debug Mode

Enable debug logging for detailed diagnostics:

```bash
# Set log level to DEBUG
export LOG_LEVEL=DEBUG

# Start application
java -jar target/simplicity-standalone.jar

# Or in Docker
docker run -e LOG_LEVEL=DEBUG ...

# Check logs
tail -f logs/app.log | grep DEBUG
```

---

*Last updated: 2026-01-22*
