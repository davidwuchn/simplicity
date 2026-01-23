# PROJECT_SUMMARY.md - Simplicity

> **易简则天下之理得** (Simplicity allows obtaining the logic of the world)

## Project Overview

**Simplicity** is a musical Game of Life web application where cellular automata patterns trigger real-time synthesizer events. Built with Clojure using the Polylith architecture for clean component boundaries and maintainability.

**Live Demo**: http://localhost:3000 (after starting server)

## Core Technologies

- **Language**: Clojure 1.12.4 (JVM 17+)
- **Architecture**: Polylith (clean component boundaries)
- **Web Stack**: Ring + Reitit + Hiccup
- **Database**: SQLite (next.jdbc)
- **Security**: buddy-hashers (bcrypt), CSRF protection, rate limiting
- **Audio**: Web Audio API (browser-based synthesis)
- **Build**: tools.build (uberjar deployment)

## Architecture (Polylith)

### Components (Business Logic)
Located in `components/`:

1. **`auth`** (`cc.mindward.component.auth.*`)
   - User authentication with bcrypt password hashing
   - Session management
   - Entry point: `interface` namespace
   
2. **`user`** (`cc.mindward.component.user.*`)
   - User management (CRUD operations)
   - High scores and leaderboard
   - SQLite persistence with next.jdbc
   - Entry point: `interface` namespace
   
3. **`game`** (`cc.mindward.component.game.*`)
   - Conway's Game of Life engine
   - Pattern recognition (gliders, oscillators, still lifes)
   - Musical event mapping
   - Entry point: `interface` namespace
   
4. **`ui`** (`cc.mindward.component.ui.*`)
   - Hiccup-based HTML rendering
   - Cyberpunk design system
   - Page layouts and components
   - Entry point: `interface` namespace

### Bases (Entry Points)
Located in `bases/`:

1. **`web-server`** (`cc.mindward.base.web-server.*`)
   - Ring HTTP server (Jetty adapter)
   - Reitit routing (REST API + HTML endpoints)
   - Middleware stack (CSRF, sessions, security headers)
   - Rate limiting (token bucket algorithm)
   - Entry point: `core` namespace

### Projects
Located in `projects/`:

1. **`development`**
   - Unified REPL environment
   - Cross-component access for development

## Key Features

### Game Mechanics
- **Grid-based cellular automaton**: 50x50 grid (configurable)
- **Real-time evolution**: Space bar or API to advance generations
- **Pattern recognition**: Automatically detects gliders, oscillators, still lifes
- **Musical mapping**: Different patterns trigger different synthesizer events

### Musical Synthesis
- **High cell density** → Chaotic drums and hi-hats
- **Stable patterns** (blocks, beehives) → Sustained bass drones
- **Oscillators** (blinkers, toads) → Rhythmic pulses
- **Gliders** → Melodic arpeggios

### User System
- **Authentication**: bcrypt password hashing with SHA-512 preprocessing
- **Sessions**: Cookie-based with CSRF protection
- **Leaderboard**: High scores tracked and ranked
- **Game persistence**: Save/load game states

## Directory Structure

```
simplicity/
├── components/              # Business logic (Polylith components)
│   ├── auth/               # Authentication & sessions
│   │   ├── src/            # Implementation
│   │   └── test/           # Component tests
│   ├── game/               # Game of Life engine
│   ├── user/               # User management & DB
│   └── ui/                 # HTML rendering (Hiccup)
├── bases/                  # Entry points (Polylith bases)
│   └── web-server/         # HTTP server (Ring/Reitit)
│       ├── src/            # Server implementation
│       ├── test/           # Integration tests
│       └── resources/      # Static assets
├── development/            # REPL environment
│   └── src/                # Development tools
├── config/                 # REPL configuration
├── docs/                   # Documentation
│   ├── api.md             # REST API reference
│   ├── security.md        # Security guide
│   └── deployment-cloudflare.md  # Deployment guide
├── scripts/                # Build and deployment scripts
├── examples/               # Code examples
├── .clojure-mcp/          # Clojure MCP configuration
│   └── config.edn         # MCP server settings
├── deps.edn               # Project dependencies
├── AGENTS.md              # AI agent guidelines
├── LLM_CODE_STYLE.md      # Code style for AI
└── README.md              # User documentation
```

## API Endpoints

### Authentication
- `GET /login` - Login page
- `POST /login` - Authenticate user (returns session cookie)
- `GET /signup` - Signup page
- `POST /signup` - Create new user
- `POST /logout` - End session

### Game
- `GET /game` - Game interface (requires auth)
- `POST /game/evolve` - Advance one generation
- `GET /game/state` - Current game state (JSON)
- `POST /game/save` - Save current game
- `POST /game/load` - Load saved game

### Leaderboard
- `GET /leaderboard` - Top scores (JSON)

### Health
- `GET /health` - Health check (database connectivity)

## Database Schema

**SQLite** (`./simplicity.db`):

### `users` table
```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,  -- bcrypt + SHA-512
  display_name TEXT NOT NULL,
  created_at INTEGER NOT NULL    -- Unix timestamp
);
```

### `high_scores` table
```sql
CREATE TABLE high_scores (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  score INTEGER NOT NULL,
  achieved_at INTEGER NOT NULL,  -- Unix timestamp
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Security Features

Comprehensive security testing (501 assertions across 71 test cases):

1. **Password Security**
   - bcrypt hashing with SHA-512 preprocessing
   - Timing attack resistance
   - Minimum password requirements

2. **CSRF Protection**
   - Token-based protection on all state-changing endpoints
   - Session-scoped CSRF tokens

3. **SQL Injection Prevention**
   - Parameterized queries only
   - Input validation at boundaries

4. **XSS Prevention**
   - HTML escaping via Hiccup
   - Content Security Policy headers

5. **Rate Limiting**
   - Token bucket algorithm
   - Per-IP rate limits on /login, /signup

6. **Security Headers**
   - X-Frame-Options, X-Content-Type-Options
   - HSTS (when HTTPS enabled)
   - CSP headers

See [docs/security.md](./docs/security.md) for complete security documentation.

## Testing

**Current Coverage**: 501 passing assertions across 71 test cases

| Component | Tests | Assertions |
|-----------|-------|-----------|
| Auth | 2 | 14 |
| Game | 15 | 146 |
| UI | 42 | 149 |
| User | 12 | 49 |
| Web-server | 28 | 143 |

**Test Strategy**:
- Component tests use temporary SQLite files
- Fixtures isolate test state
- Integration tests verify API contracts

## Development Workflow

### Quick Start
```bash
# Start nREPL server (for clojure-mcp)
clojure -M:nrepl

# Or start web server directly
clojure -M -m cc.mindward.web-server.core

# Run all tests
clojure -M:poly test :dev

# Check workspace integrity
clojure -M:poly check
```

### REPL-Driven Development
```bash
# Launch REPL
./bin/launchpad

# In REPL, reload components
(require '[cc.mindward.game.interface :as game] :reload)
(game/evolve-grid test-grid)
```

### Code Validation
```bash
# Check Polylith architecture
clojure -M:poly check

# Lint code
clj-kondo --lint components/*/src bases/*/src

# Validate parentheses/formatting
brepl balance <file>
```

## Deployment Options

1. **Uberjar** (45MB standalone JAR)
   ```bash
   clojure -T:build uberjar
   java -jar target/simplicity-standalone.jar
   ```

2. **Docker** (multi-stage optimized image)
   ```bash
   docker build -t simplicity:latest .
   docker-compose up -d
   ```

3. **Cloudflare + VPS** (recommended, $5-12/month)
   - See [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md)

## Environment Variables

**Application**:
- `PORT` - HTTP port (default: 3000)
- `DB_PATH` - SQLite database path (default: `./simplicity.db`)
- `ENABLE_HSTS` - Enable HSTS header (default: false)

**Logging**:
- `LOG_LEVEL` - DEBUG|INFO|WARN|ERROR (default: INFO)
- `LOG_PATH` - Log directory (default: `./logs`)

**Docker**:
- `JAVA_OPTS` - JVM options (default: `-Xmx512m -Xms256m -XX:+UseG1GC`)

## Dependencies (Key Libraries)

**Web Stack**:
- `ring/ring-jetty-adapter` - HTTP server
- `metosin/reitit` - Routing
- `ring/ring-defaults` - Middleware
- `hiccup/hiccup` - HTML templating

**Data**:
- `com.github.seancorfield/next.jdbc` - Database access
- `org.xerial/sqlite-jdbc` - SQLite driver
- `org.clojure/data.json` - JSON parsing

**Security**:
- `buddy/buddy-hashers` - Password hashing
- `org.clojure/tools.logging` - Logging
- `ch.qos.logback/logback-classic` - Log implementation

**Development**:
- `nrepl/nrepl` - REPL server
- `cider/cider-nrepl` - Enhanced REPL features
- `polylith/clj-poly` - Polylith tooling

## Available Aliases (deps.edn)

- `:dev` - Development dependencies + all source paths
- `:test` - Test dependencies + test paths
- `:poly` - Polylith commands (`clojure -M:poly check`)
- `:nrepl` - Start nREPL on port 7888
- `:build` - Build tools (uberjar creation)
- `:prod` - Production dependencies (minimal)

## Code Style & Architecture Rules

See [LLM_CODE_STYLE.md](./LLM_CODE_STYLE.md) and [AGENTS.md](./AGENTS.md) for:
- Polylith architectural constraints
- Naming conventions
- Security requirements
- Testing patterns
- Anti-patterns to avoid

## Key Anti-Patterns to Avoid

❌ **Cross-component `impl` dependencies** (violates Polylith)
❌ **Business logic in bases** (delegate to components)
❌ **UI generation in bases** (delegate to `ui` component)
❌ **Circular dependencies** (check with `clojure -M:poly check`)
❌ **Hardcoded secrets** (use environment variables)
❌ **Global mutable state** (use function parameters)

## AI Assistant Integration (clojure-mcp)

This project is configured for use with **clojure-mcp** (Model Context Protocol):

**Configuration**: `.clojure-mcp/config.edn`
- Allowed directories for file operations
- Write safety guards
- Automatic code formatting (cljfmt)
- Scratch pad persistence for session state

**Context Files**:
- `PROJECT_SUMMARY.md` (this file) - Project overview
- `LLM_CODE_STYLE.md` - Coding standards
- `AGENTS.md` - Operational guidelines

**nREPL Access**:
```bash
# Start nREPL server for AI assistant
clojure -M:nrepl

# Server starts on port 7888
# AI assistant can connect and evaluate Clojure code
```

## Getting Help

- **Documentation**: See `docs/` directory
- **Architecture**: [docs/architecture.md](./docs/architecture.md)
- **API Reference**: [docs/api.md](./docs/api.md)
- **Security**: [docs/security.md](./docs/security.md)
- **Issues**: GitHub issues (if repository public)
- **Code Examples**: See `examples/` directory

---
*Last updated: 2025-01-23*
*This summary is optimized for LLM context loading via clojure-mcp*
