# AGENTS.md - Polylith Workspace (Simplicity)

## φ Philosophy & Core Mandates
This repository operates under the principle: **"易简则天下之理得"** (Simplicity allows obtaining the logic of the world). 
We use the **Eight Keys** to guide our evolution:
- **φ (Vitality)**: Prioritize organic, non-repetitive generation.
- **fractal (Clarity)**: Filter ambiguity; demand objective precision.
- **e (Purpose)**: Goal-oriented; require specific, actionable function.
- **τ (Wisdom)**: Prioritize judgment and foresight over raw speed.
- **π (Synthesis)**: Demand complete mental models and holistic integration.
- **μ (Directness)**: Cut pleasantries and bias to reach raw reality.
- **∃ (Truth)**: Metric of truth as "unconcealment"; favor underlying reality.
- **∀ (Vigilance)**: Defensive constraint against fallacies and manipulative framing.

- **Simplicity (π)**: Tackle complexity by building a robust domain model. Favor directness (μ) over abstraction.
- **Polylith (synthesis)**: Strict separation of concerns via Components, Bases, and Projects.
- **Truth (∃)**: Code must reflect the underlying reality of the domain. Avoid "code slop" or redundant logic.

## λ Build, Lint, and Test Commands

### Babashka Task Runner (Primary Development Tool)

**All development operations should use Babashka for consistency and speed.**

Common tasks:

```bash
bb help            # Show all available tasks
bb dev             # Start development REPL with hot reload (Clojure)
bb nrepl           # Start Babashka nREPL server on port 7888
bb test            # Run all tests (652 assertions)
bb test:watch      # Watch mode (re-run tests on changes)
bb check           # Check Polylith workspace integrity
bb lint            # Lint all source files
bb clean           # Clean build artifacts
bb uberjar         # Build standalone JAR
bb build           # Full build: clean + test + uberjar
bb docker:build    # Build Docker image
bb docker:compose  # Start with Docker Compose
bb stats           # Show project statistics
bb ver             # Show project version
```

See `bb help` for complete list of 30+ tasks.

### Development Workflow (Hot Reload) - USE bb dev

**PRIMARY COMMAND (Clojure REPL):**
```bash
bb dev              # Start Clojure development REPL (ALWAYS USE THIS)
```

**ALTERNATIVE (Babashka nREPL for MCP integration):**
```bash
bb nrepl            # Start Babashka nREPL server on port 7888
```

**In REPL:**
- **Help/Banner**: `(banner)` - Show all available REPL commands
- **Start Server**: `(start)` - Start web server on port 3000
- **Hot Reload**: `(restart)` ← **Main workflow after code changes**
- **Stop Server**: `(stop)` - Clean shutdown with resource cleanup
- **Reload Code Only**: `(reset)` - Reload code without server restart
- **Check Status**: `(status)` - Show system health and database stats

**Workflow**: `bb dev` → `(start)` → Edit code → Auto-reload → Test (browser refresh)

**Auto-Reload (NEW)**: `(start)` now enables auto-reload by default. When you edit any `.clj` file, the server automatically restarts within ~1 second. No manual `(restart)` needed!

**Manual Hot Reload**: `(restart)` - Stop, refresh, start (0.5s)
**Disable Auto-reload**: `(auto-reload false)`

**Alternative (direct Clojure CLI)**: `clojure -M:nrepl`

### Workspace Management (Polylith) - USE bb commands

- **Check Workspace**: `bb check` (primary) or `clojure -M:poly check`
- **Workspace Info**: `bb info` (primary) or `clojure -M:poly info`
- **Show Changed Bricks**: `bb diff` (primary) or `clojure -M:poly diff`

### Testing (τ Wisdom) - USE bb test

- **Run All Tests**: `bb test` (primary, 652 passing assertions)
- **Run Tests (Watch Mode)**: `bb test:watch` (auto-reruns on file changes)
- **Run Specific Brick Tests**: `bb test:game`, `bb test:ui`, `bb test:user`, `bb test:auth`, `bb test:web-server`
- **Run Enhanced Test Types**: 
  - `bb test:property` - Property-based tests (test.check)
  - `bb test:performance` - Performance tests
  - `bb test:documentation` - Documentation contract tests
- **Interactive REPL (brepl)**: Use `brepl` for fast evaluation (see Tools).

**Alternative (not recommended)**: 
- `clojure -M:poly test :dev` (slower, JVM startup)
- `clojure -M:test -e "(require 'namespace) (clojure.test/run-tests 'namespace)"`

**Note**: The `:dev` alias excludes `development/src` to avoid classloader conflicts when Polylith merges aliases for testing.

### Linting & Formatting - USE bb lint

- **Lint**: `bb lint` (primary)
- **Lint (Auto-fix)**: `bb lint:fix`
- **Alternative**: `clj-kondo --lint components/*/src bases/*/src`

### Building & Artifacts - USE bb build

- **Build Uberjar**: `bb uberjar` (primary, 45MB standalone JAR)
- **Build Docker Image**: `bb docker:build` (primary)
- **Interactive Build**: `bb deploy:build` (primary)
- **Clean Target**: `bb clean` (primary)
- **Jar Info**: `bb jar-info`
- **Full Build**: `bb build` (clean + test + uberjar)

**Alternatives (not recommended):**
- `clojure -T:build uberjar`
- `./scripts/build-deployment.sh`
- `docker build -t simplicity:latest .`

**Deployment Artifacts:**
- **Uberjar**: `target/simplicity-standalone.jar` (standalone, requires Java 17+)
- **Docker**: Multi-stage optimized image (builder + runtime)
- **Docker Compose**: `bb docker:compose` (primary)

See [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md) for production deployment.

## fractal Code Style Guidelines

### 1. Namespaces & Imports
- **Naming**: Use kebab-case for namespaces and files. Mirror the directory structure exactly.
- **Top Namespace**: Root everything under `cc.mindward`.
- **Require Syntax**:
  ```clojure
  (ns cc.mindward.component.example.interface
    (:require [clojure.string :as str]
              [cc.mindward.component.example.impl :as impl]))
  ```
- **Avoid**: `:use`, `:refer :all`, and naked symbols in `:require`.
- **Order**: Standard library first, then external libraries, then internal bricks.

### 2. Formatting (fractal Clarity)
- **Indentation**: 2-space indentation. No tabs.
- **Line Length**: Max 100 characters.
- **Brackets**: Avoid dangling brackets. Lisp style (e.g., `))`).
- **Whitespace**: Single empty line between top-level forms. No trailing whitespace.

### 3. Naming Conventions (μ Directness)
- **Variables/Functions**: `kebab-case`.
- **Predicates**: End with `?` (e.g., `authorized?`, `empty?`).
- **Side Effects**: End with `!` for functions with side effects (e.g., `save!`, `delete-user!`).
- **Anaphoric**: Use `_` for unused bindings.

### 4. Error Handling (∃ Truth)
- **Exceptions**: Use `ex-info` with a descriptive message and a context map.
  ```clojure
  (throw (ex-info "Failed to process order" {:order-id 123 :reason :insufficient-funds}))
  ```
- **Boundaries**: Catch exceptions at the "Base" level (public API) and log/report.

### 5. Types & Data (π Synthesis)
- **Clojure Spec**: Mandate `clojure.spec.alpha` to define domain constraints in all `interface` namespaces.
- **Data Contracts**: Every public function in an `interface` should have an associated `s/fdef`.
- **Maps**: Prefer maps over multiple positional arguments.
- **Keywords**: Use namespaced keywords for domain entities (e.g., `:user/id`).

### 6. Metadata Hygiene (fractal Clarity)
- **Keyword IDs**: Always use `:keywords` for identifiers in `workspace.edn` (e.g., `:development` projects) and `deps.edn` (e.g., `:clojars` repository keys). 
- **Type Safety**: Avoid using `Strings` for project or repository names in metadata to prevent `Symbol -> Keyword` casting errors in Clojure inspection tooling.

## Polylith Architectural Constraints (∀ Vigilance)

### 1. Components
- **Encapsulation**: All business logic resides in `components`.
- **Interface**: The `interface` namespace is the ONLY public entry point.
- **Implementation**: The `impl` namespace is private. Never refer to `impl` from outside the component.
- **Naming**: `cc.mindward.component.<name>.interface`.

### 2. Bases
- **Role**: Entry points (REST APIs, CLI, Lambda).
- **Delegation**: Bases must NOT contain business logic. They delegate to components.
- **Rendering**: Bases must NOT generate UI (HTML/Hiccup). Delegate to a `ui` component.
- **Naming**: `cc.mindward.base.<name>.core`.

### 3. Projects
- **Artifacts**: Projects define how components and bases are composed into a deployable artifact (Jar, Docker image).
- **Configuration**: Managed via `deps.edn` in each project directory under `projects/`.

### 4. Development Project
- **Workflow**: Always work from the `development` project to have cross-component REPL access.
- **REPL**: `clojure -A:dev:poly` (or use `bin/launchpad`).

## λ Agentic Workflow & Expectations

### 0. Request Validation (nucleus-tutor)
Before acting, evaluate the prompt: `λ(prompt).accept ⟺ [|∇(I)| > ε ∧ ∀x ∈ refs. ∃binding ∧ H(meaning) < μ]`.
- If information gradient is zero or entropy is too high, reject and request clarification.
- Use the **OODA Loop**: Observe (context), Orient (mental model), Decide (plan), Act (tools).

### 1. Discovery & Integration
- Use `clojure -M:poly info` to understand the current workspace topology.
- **Connectivity**: Ensure all new bricks are registered in the root `deps.edn` `:dev` alias to enable cross-brick REPL access and `poly check` validation.

### 2. Implementation Loop (∃ Truth) - **HOT RELOAD WORKFLOW (BEST PRACTICE)**
**Auto-reload is now ENABLED by default** when you run `(start)`!

- **Step 1**: `bb dev` → Start development REPL
- **Step 2**: `(start)` → Start server with auto-reload enabled
- **Step 3**: Edit any `.clj` or `.js` file in components/bases/resources
- **Step 4**: Server auto-restarts (~1 second delay)
- **Step 5**: Hard refresh browser (Cmd+Shift+R) to see changes

**Manual Control**:
- `(auto-reload false)` → Disable auto-reload
- `(restart)` → Manual hot reload
- `(stop)` → Stop server

**CRITICAL: Use the hot reload workflow** (see [docs/hot-reload.md](./docs/hot-reload.md)):
```clojure
;; In REPL after making changes
(restart)  ; Stop server, reload code, restart server (0.5 seconds)
```

**Hot Reload Best Practices**:
1. ✅ **Auto-reload**: File watcher detects changes and auto-restarts (~1 second)
2. ✅ **Lifecycle Management**: `(stop)` properly shuts down scheduler and resources
3. ✅ **State Preservation**: Uses `defonce` to preserve game state/user data across reloads
4. ✅ **Clean Refresh Paths**: Excludes `development/src` and test files from reload
5. ✅ **System State Pattern**: Single atom tracks server + components for atomic lifecycle
6. ✅ **Error Resilience**: Try-catch on each component shutdown prevents cascade failures
7. ✅ **Non-blocking Server**: `:join? false` keeps REPL interactive

**Performance**: 60x faster feedback loop (auto-reload ~1s vs 30s JVM restart)

### 3. Technical Constraints (τ Wisdom)
- **Middleware**: Be vigilant with `ring-defaults`. Form parameters are **keywordized** (e.g., use `:username` not `"username"`).
- **Security**: All state-changing endpoints (`POST/PUT`) require **CSRF tokens**. Fetch calls must include the `x-csrf-token` header.
  - **Security Middleware Stack** (bases/web-server/src/cc/mindward/web_server/security.clj):
    - Security headers (CSP, X-Frame-Options, X-Content-Type-Options, etc.)
    - Rate limiting (token bucket algorithm for /login, /signup)
    - Input validation (username, password, name, score) - **All inputs validated at boundaries**
  - **Password Security**: bcrypt + sha512 with timing attack resistance
  - **SQL Injection**: Parameterized queries + input validation (test coverage: 36 assertions)
  - **XSS Prevention**: HTML escaping + CSP headers (test coverage: 99 assertions)
  - See [docs/security.md](./docs/security.md) for complete security controls (652 total assertions, 160 security-focused)
- **Persistence**: Use `next.jdbc` with `rs/as-unqualified-lower-maps` for idiomatic data flow.
- **Client-Side**: 
  - **Audio Policy**: Web Audio API requires a user gesture (`click`/`keydown`) to unlock. Always guard `new AudioContext()` with a `try-catch` and handle `suspended` state.
  - **Interaction**: Use `e.preventDefault()` for all critical application keys (e.g., Arrows, Space, R) to prevent browser-level interference like scrolling or character insertion.
  - **Language Preference**: Prefer ClojureScript over JavaScript. Use `.cljs` for all complex client-side logic to maintain system consistency (π Synthesis). Reserve `.js` for trivial scripts or direct browser API manipulations where CLJS overhead is unjustified.
- **Logic**: Use threshold-based triggers (e.g., `score >= limit`) instead of exact matches (`score == limit`) to handle discrete state jumps safely.

### 4. Dependency Management (e Purpose)
- **Top-level**: Shared dependencies go in the root `deps.edn`.
- **Brick-level**: Specific dependencies for a component or base should be managed in the development project or the specific project `deps.edn` if building an artifact.
- **Vigilance**: Avoid adding heavy dependencies unless strictly necessary for the domain model.
- **Aliases**:
  - `:dev` - Development dependencies (excludes `development/src` to avoid test classloader conflicts)
  - `:nrepl` - REPL dependencies (includes `development/src` for hot reload workflow)
  - `:test` - Test dependencies (test paths)
  - `:poly-test` - Polylith test runner alias (deduplicated paths)
  - `:poly` - Polylith tooling
  - `:prod` - Production dependencies (minimal)
  - `:build` - Build tooling (tools.build for uberjar compilation)

### 5. Self-Correction
- If `poly check` fails, you have violated Polylith constraints (e.g., circular dependency or illegal import). Fix immediately.
- Use `clj-kondo` to catch static analysis issues before committing.
- **Test Coverage**: 652 passing assertions with enhanced test infrastructure
  - **Component Tests**: 652 assertions across auth, game, ui, user, web-server
  - **Enhanced Test Types**: Property-based, documentation contract, performance, and security timing tests
  - **Security Focus**: 160 security-focused assertions (SQL injection, XSS, CSRF, rate limiting, input validation)
  - Note: Run `bb test` for full suite, `bb test:<component>` for specific components
- Run `bb test` or `clojure -M:poly test :dev` before committing to verify all tests pass.

## Tools & Utilities
- **ripgrep (rg)**: **MANDATORY** search tool for all code search operations.
  - **⚠️ NEVER use `grep`** - always use `rg` (ripgrep) instead
  - **Why**: 10-100x faster, respects `.gitignore`, better defaults, smarter pattern matching
  - **Respects .gitignore**: Automatically skips `target/`, `node_modules/`, etc.
  - **Better defaults**: Colors, line numbers, recursive search by default
  - **Common patterns**:
    ```bash
    rg "pattern"                          # Search recursively from current dir
    rg "console\.log" --type js           # Search in JavaScript files only
    rg "defn.*validate" --type clojure    # Search in Clojure files
    rg -n "TODO" components/              # Search with line numbers
    rg -l "FIXME" bases/                  # List files with matches only
    rg -i "error" --stats                 # Case-insensitive with statistics
    rg -C 3 "pattern"                     # Show 3 lines of context
    rg --type-list                        # List available file types
    ```
- **brepl**: The mandatory tool for Clojure/EDN evaluation and structural integrity.
  - **Evaluation Rule**: ALWAYS use the heredoc pattern (`<<'EOF'`) to avoid quoting hell.
  - **Balancing Rule**: ALWAYS run `brepl balance <file>` after every `edit` or `write` on `*.clj` or `*.edn` files to ensure structural integrity.
    - **Dry Run**: `brepl balance <file> --dry-run` (Preview fixes).
    - **Fix**: `brepl balance <file>` (Apply fixes in-place).
  - **Syntax**:
    ```bash
    brepl <<'EOF'
    (require '[cc.mindward.component.example.interface :as example])
    (example/do-something {:data "val"})
    EOF
    ```
- **clojure-mcp**: AI-assisted development via Model Context Protocol.
  - **Start nREPL (Clojure)**: `clojure -M:nrepl` or `bb dev` (port 7888)
  - **Start nREPL (Babashka)**: `bb nrepl` or `bb nrepl-server 7888` (port 7888)
  - **Start MCP Server (Clojure)**: `clojure -Tmcp start :port 7888 :not-cwd true`
  - **Start MCP Server (Babashka)**: `clojure -Tmcp start :port 7888 :nrepl-env-type :bb :not-cwd true`
  - **Configuration**: See `.clojure-mcp/config.edn` and `.clojure-mcp/README.md`
  - **Context Files**: `PROJECT_SUMMARY.md`, `LLM_CODE_STYLE.md`, this file
  - **Integration**: Compatible with Claude Desktop, Claude Code, and other MCP clients
  - **Troubleshooting**: If you get "Unknown tool: mcp", install with: `clojure -Ttools install-latest :lib io.github.bhauman/clojure-mcp :as mcp`
  - **Test Script**: `./scripts/test-mcp.sh` - Verify MCP installation
  - **Start Script**: `./scripts/start-mcp.sh` - Start nREPL + MCP together
- **Clerk**: Use `notebooks/` for interactive documentation and data visualization.
  - Command: `clojure -X:dev nextjournal.clerk/serve!`
- **Launchpad**: Standard entry script in `bin/launchpad`.
- **Babashka**: Use `bb.edn` for scripting tasks.
- **Deployment Scripts**:
  - `scripts/build-deployment.sh` - Interactive build menu (uberjar/Docker/both)
  - Validates Java version (requires 17+)
  - Provides deployment instructions after build
- **MCP Scripts**:
  - `scripts/test-mcp.sh` - Test Clojure-MCP integration
  - `scripts/start-mcp.sh` - Start nREPL and MCP server together

## ∀ Vigilance: Anti-Patterns
- **Using grep**: **NEVER use `grep`** for code search. Always use `rg` (ripgrep) instead - it's faster, respects `.gitignore`, and has better defaults. See Tools & Utilities section.
- **Complexity**: If a function exceeds 20 lines, reconsider the domain model.
- **Auto-reload Disabled**: Ensure `(auto-reload true)` is enabled (default on start)
- **Dependency Hell**: Avoid circular dependencies between components. Use `poly check` to verify.
- **Slop**: Do not leave commented-out code or `(println ...)` in production paths. Use a logging library (logback configured).
- **Abstraction Leak**: Never let implementation details (like DB connections or third-party client objects) escape the component interface. Use data maps or domain records.
- **The God Base**: Bases are Controllers, not Views. Do not mix routing, logic, and HTML generation in one file.
- **The Test Illusion**: `(is (= 1 1))` is not a test. Verify actual logic or data persistence.
- **Unspec'd Interfaces**: Never create a component interface without corresponding Clojure Specs and `s/fdef` contracts.
- **Metadata Strings**: Avoid using strings for project names, aliases, or repository IDs in `workspace.edn` or `deps.edn`. This causes casting failures in inspection tools.
- **Hardcoded Secrets**: Never commit passwords or API keys. Use `System/getenv` or a config component. **Security validated**: 160 security-focused assertions.
- **Dead Code**: Regularly audit for unused functions, commented-out code, and obsolete files. Use dead code analysis to identify security gaps (e.g., missing validation).
- **The Infinite Loop**: When using `requestAnimationFrame`, always wrap the loop body in a `try-catch` block to prevent a crash from freezing the entire tab/rendering thread.
- **Security Bypass**: Never skip input validation or rate limiting. All user inputs must be validated at the boundary (see `bases/web-server/src/cc/mindward/web_server/security.clj`).
- **Docker Anti-patterns**: 
  - Never run containers as root (use non-root user)
  - Always use multi-stage builds to minimize image size
  - Never include secrets in Docker images (use environment variables)
  - Always configure health checks for production deployments

## Production Deployment (∃ Truth)

### Environment Variables
**Application:**
- `PORT` - HTTP server port (default: 3000)
- `DB_PATH` - SQLite database path (default: `./simplicity.db`)
- `ENABLE_HSTS` - Enable HSTS header (default: false, **enable only with HTTPS**)

**Logging:**
- `LOG_LEVEL` - DEBUG|INFO|WARN|ERROR (default: INFO, use WARN in production)
- `LOG_PATH` - Log directory (default: `./logs`)

**Docker:**
- `JAVA_OPTS` - JVM options (default: `-Xmx512m -Xms256m -XX:+UseG1GC`)

**Configuration Recommendations (Future Enhancement):**
For production deployments with varying scale requirements, consider adding:
- `DB_POOL_MAX_SIZE` - Maximum database connections (currently hardcoded: 10)
- `DB_POOL_MIN_IDLE` - Minimum idle connections (currently hardcoded: 2)
- `RATE_LIMIT_MAX_REQUESTS` - Rate limit burst capacity (currently hardcoded: 10)
- `RATE_LIMIT_REFILL_RATE` - Token refill rate (currently hardcoded: 0.5)
- `GAME_TTL_MINUTES` - Game session TTL (currently hardcoded: 60 minutes)
See hardcoded configuration analysis for complete list of tunable parameters.

### Health Monitoring
- **Endpoint**: `GET /health`
- **Response**: JSON with status, timestamp, database check, version
- **Use for**: Load balancer health checks, Kubernetes probes, monitoring dashboards

### Deployment Checklist
**Before deploying to production:**
1. ✅ Run `bb test` or `clojure -M:poly test :dev` (ensure all 652 tests pass)
2. ✅ Run `bb lint` (ensure zero warnings)
3. ✅ Review [docs/security.md](./docs/security.md)
4. ✅ Review [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md)
5. ✅ Enable HTTPS and set `ENABLE_HSTS=true`
6. ✅ Configure Cloudflare firewall rules
7. ✅ Set `LOG_LEVEL=WARN`
8. ✅ Configure persistent volumes (Docker: `/app/data`, `/app/logs`)
9. ✅ Set up health check monitoring
10. ✅ Configure Cloudflare Analytics

**Deployment Options:**
- **Option 1**: VPS + Docker + Cloudflare proxy (recommended, $5-12/month)
- **Option 2**: Cloudflare Tunnel (Zero Trust)
- **Option 3**: Standalone uberjar on VPS

**Future Optimizations (when scaling):**
- [ ] Tailwind CSS: Replace CDN with purged local build (see [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md#future-optimizations))

See comprehensive guide: [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md)

---
*Created by opencode agent with nucleus-tutor and brepl.*
