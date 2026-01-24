# Simplicity (π)

> **易简则天下之理得** (Simplicity allows obtaining the logic of the world)

**A musical Game of Life web application** where cellular automata patterns trigger real-time synthesizer events. Built with Clojure using the Polylith architecture, this project demonstrates domain-driven design with clear component boundaries and generative music synthesis.

## What is Simplicity?

Simplicity is an interactive web application that combines:
- **Conway's Game of Life**: Classic cellular automaton simulation
- **Generative Music**: Patterns on the board trigger synthesizer events (bass, drums, ambient pads)
- **Competitive Gameplay**: Save games, track high scores, compete on the leaderboard
- **Cyberpunk Aesthetic**: Retro-futuristic UI with neon colors and glitch effects

Try creating patterns and hear how they evolve into music!

## 🧱 Architecture (Polylith)

This workspace follows the [Polylith architecture](https://polylith.gitbook.io/) to maintain clean separation of concerns:

**Components** (business logic):
- `auth` - User authentication with bcrypt password hashing
- `user` - User management, high scores, leaderboard (SQLite)
- `game` - Conway's Game of Life engine with pattern recognition & musical triggers
- `ui` - Hiccup-based HTML rendering with cyberpunk design system

**Bases** (entry points):
- `web-server` - Ring/Reitit HTTP server with session management and JSON API

**Projects**:
- `development` - Unified REPL environment for all components

## 🚀 Quick Start

### Prerequisites
- **Java 17+**
- [Clojure CLI](https://clojure.org/guides/install_clojure) (1.11+)
- **[Babashka](https://babashka.org/)** (required for bb commands)

### Development (Hot Reload Workflow)

**Start development environment:**

```bash
# Start development REPL with hot reload
bb dev

# In REPL:
user=> (start)     ; Start web server on port 3000
user=> (restart)   ; Hot reload after making changes (1-2 sec!)
user=> (stop)      ; Stop server
user=> (help)      ; Show all commands
```

**Without Babashka (fallback):**

```bash
clojure -M:nrepl
# Same REPL commands as above
```

**Open in browser**: http://localhost:3000

**Workflow**: Edit code → `(restart)` in REPL → Test in browser (instant feedback!)

See [docs/hot-reload.md](./docs/hot-reload.md) for complete guide.

### Quick Commands (Babashka)

```bash
bb help            # Show all 30+ available commands
bb test            # Run all tests (652 assertions)
bb test:watch      # Auto-rerun tests on file changes
bb check           # Check Polylith workspace integrity
bb build           # Full build: clean + test + uberjar
bb docker:build    # Build Docker image
bb stats           # Show project statistics
```

## 🎮 How to Play

1. **Sign up** at `/signup` (username + password + display name)
2. **Login** and navigate to `/game`
3. **Draw patterns** on the grid (click/drag cells)
4. **Press Space** to evolve the pattern
5. **Listen** as patterns trigger musical events
6. **Save your game** and compete for high scores

### Musical Mapping

Different Game of Life patterns trigger different sounds:
- **High cell density** → Chaotic drums and hi-hats
- **Stable patterns** (blocks, beehives) → Sustained bass drones
- **Oscillators** (blinkers, toads) → Rhythmic pulses
- **Gliders** → Melodic arpeggios

## 🛠 Development Commands

### Using Babashka (Recommended)

Babashka provides a unified task runner for all development operations:

```bash
bb help           # Show all available commands
bb dev            # Start development REPL with hot reload
bb test           # Run all tests (611 assertions)
bb test:watch     # Watch mode (re-run tests on file changes)
bb check          # Check Polylith workspace integrity
bb lint           # Lint all source files
bb clean          # Clean build artifacts
bb uberjar        # Build standalone JAR
bb build          # Full build (clean + test + uberjar)
bb docker:build   # Build Docker image
bb stats          # Show project statistics
```

See `bb help` for complete list of tasks.

### Direct Clojure Commands (if not using Babashka)

| Task | Babashka (Recommended) | Direct Clojure |
| :--- | :--- | :--- |
| **Check Workspace** | `bb check` | `clojure -M:poly check` |
| **Workspace Info** | `bb info` | `clojure -M:poly info` |
| **Run All Tests** | `bb test` | `clojure -M:poly test :dev` |
| **Run Component Tests** | `bb test:game` | `clojure -M:poly test brick:game` |
| **Run Property Tests** | `bb test:property` | - |
| **Run Performance Tests** | `bb test:performance` | - |
| **Run Documentation Tests** | `bb test:documentation` | - |
| **Lint Code** | `bb lint` | `clj-kondo --lint components/*/src bases/*/src` |
| **Launch REPL** | `bb dev` | `clojure -M:nrepl` |
| **Build Uberjar** | `bb uberjar` | `clojure -T:build uberjar` |

### Development Workflow

The workspace supports **REPL-driven development** with hot reload:

1. Start REPL: `bb dev`
2. In REPL, start server: `(start)`
3. Edit code in your editor
4. Hot reload: `(restart)` ← 1-2 second feedback loop!
5. Test at http://localhost:3000

**Editor Integration:**
- Connect to nREPL (port in `.nrepl-port`)
- Reload components: `(require '[cc.mindward.game.interface :as game] :reload)`
- Test functions interactively in REPL

**AI-Assisted Development:**
- Uses clojure-mcp for integration with Claude/LLMs
- See `docs/clojure-mcp-integration.md` for setup

## 📁 Project Structure

```
simplicity/
├── components/          # Business logic components
│   ├── auth/           # Authentication & sessions
│   ├── game/           # Game of Life engine
│   ├── user/           # User management & DB
│   └── ui/             # HTML rendering (Hiccup)
├── bases/
│   └── web-server/     # HTTP server (Ring/Reitit)
├── development/        # Unified REPL environment
├── config/             # REPL configuration
├── docs/               # Documentation (API, architecture)
├── examples/           # Code examples
└── AGENTS.md           # AI agent operational guidelines
```

## 🧪 Testing

**Current test coverage**: 652 passing assertions across all test cases with enhanced test infrastructure including property-based testing, documentation contract tests, and security timing tests.

| Component | Tests | Assertions |
| :--- | :---: | :---: |
| Auth | 3 | 25 |
| Game | 13 | 136 |
| UI | 70 | 265 |
| User | 12 | 49 |
| Web-server | 37 | 177 |

Run tests:
```bash
# Using Babashka (recommended)
bb test              # All tests (652 assertions)
bb test:watch        # Watch mode (auto-rerun on changes)
bb test:game         # Game component tests
bb test:user         # User component tests
bb test:auth         # Auth component tests
bb test:ui           # UI component tests
bb test:web-server   # Web-server base tests
bb test:property     # Property-based tests (test.check)
bb test:performance  # Performance tests
bb test:documentation # Documentation contract tests

# Using Clojure directly
clojure -M:poly test :dev              # All tests
clojure -M:poly test brick:game        # Specific component
clojure -M:poly test brick:user        # Specific component
```

Test files follow Polylith conventions:
- `components/<name>/test/cc/mindward/<name>/<namespace>_test.clj`
- Use fixtures for database isolation (see `user/interface_test.clj`)

**Enhanced Test Infrastructure:**

The project now includes comprehensive test types aligned with the Eight Keys philosophy:

| Test Type | Purpose | Philosophy Applied |
|-----------|---------|-------------------|
| **Property-based Tests** | Generative testing with `test.check` for mathematical properties | φ (Vitality) - Organic exploration of edge cases |
| **Documentation Tests** | Verify code behavior matches documented contracts | π (Synthesis) - Complete mental models |
| **Performance Tests** | Measure evolution speed, memory usage, concurrent access | τ (Wisdom) - Foresight about scalability |
| **Security Tests** | Timing attack prevention, SQL injection, XSS, CSRF | ∀ (Vigilance) - Defensive testing |
| **Integration Tests** | Real database testing with fixtures | ∃ (Truth) - Verify actual behavior |

**Security Testing:**
- SQL injection prevention
- XSS escaping verification  
- CSRF protection
- Session security
- Rate limiting
- Input validation
- Timing attack prevention

See [docs/security.md](./docs/security.md) for complete security documentation.

## 🚢 Deployment

### Quick Deploy (Babashka)

```bash
# Interactive build menu
bb deploy:build

# Or build directly
bb build              # Full build: clean + test + uberjar
bb docker:build       # Build Docker image
bb docker:compose     # Start with Docker Compose
```

### Quick Deploy (Scripts)

**Interactive build script:**
```bash
./scripts/build-deployment.sh
```

Choose from:
1. **Uberjar** (45MB standalone JAR)
2. **Docker image** (multi-stage, optimized)
3. **Both**

### Option 1: Uberjar Deployment

Build standalone JAR:
```bash
bb uberjar
# or
clojure -T:build uberjar
```

Run locally:
```bash
java -jar target/simplicity-standalone.jar
```

Deploy to server:
```bash
scp target/simplicity-standalone.jar user@server:/opt/simplicity/
ssh user@server 'java -jar /opt/simplicity/simplicity-standalone.jar'
```

### Option 2: Docker Deployment

Build image:
```bash
bb docker:build
# or
docker build -t simplicity:latest .
```

Run with Docker:
```bash
docker run -d -p 3000:3000 \
  -v simplicity-data:/app/data \
  -v simplicity-logs:/app/logs \
  --name simplicity \
  simplicity:latest
```

Or use docker-compose:
```bash
docker-compose up -d
```

### Option 3: Cloudflare Deployment

**Recommended**: VPS + Docker + Cloudflare as CDN/proxy

See [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md) for complete guide:
- VPS deployment steps (DigitalOcean, Linode, Vultr)
- Cloudflare DNS & SSL configuration
- Cloudflare Tunnel setup (Zero Trust)
- Performance optimization
- Monitoring & analytics
- Cost breakdown ($5-12/month)

### Environment Variables

**Application:**
- `PORT` - HTTP server port (default: 3000)
- `DB_PATH` - SQLite database path (default: `./simplicity.db`)
- `ENABLE_HSTS` - Enable HSTS header (default: false, enable only with HTTPS)

**Logging:**
- `LOG_LEVEL` - Logging level: DEBUG, INFO, WARN, ERROR (default: INFO)
- `LOG_PATH` - Directory for log files (default: `./logs`)

**Docker:**
- `JAVA_OPTS` - JVM options (default: `-Xmx512m -Xms256m -XX:+UseG1GC`)

### Health Check

Monitor application health:
```bash
curl http://localhost:3000/health
```

Response:
```json
{
  "status": "healthy",
  "timestamp": 1769085000000,
  "checks": {
    "database": {
      "status": "up",
      "responseTimeMs": 5
    }
  },
  "version": "1.0.0"
}
```

**Use for:**
- Load balancer health checks
- Kubernetes liveness/readiness probes
- Monitoring dashboards

### Production Checklist

**Security:**
- [ ] Review [docs/security.md](./docs/security.md) for complete security guidelines
- [ ] Review [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md) for deployment guide
- [ ] Enable HTTPS and configure `ENABLE_HSTS=true`
- [ ] Configure Cloudflare firewall rules
- [ ] Set `LOG_LEVEL=WARN` to reduce verbosity

**Configuration:**
- [ ] Set `PORT` for your environment
- [ ] Set `DB_PATH` to persistent storage location
- [ ] Ensure Java 17+ on target system (or use Docker)
- [ ] Configure persistent volumes (Docker: `/app/data`, `/app/logs`)

**Monitoring:**
- [ ] Configure health check monitoring (`/health` endpoint)
- [ ] Set up Cloudflare Analytics
- [ ] Monitor authentication failures (potential brute force)
- [ ] Track rate limit violations (429 responses)

## 📚 Documentation

- **[AGENTS.md](./AGENTS.md)** - AI agent operational guidelines & code style
- **[PROJECT_SUMMARY.md](./PROJECT_SUMMARY.md)** - Project summary for AI assistants (clojure-mcp)
- **[LLM_CODE_STYLE.md](./LLM_CODE_STYLE.md)** - Code style guide for AI-assisted development
- **[docs/clojure-mcp-integration.md](./docs/clojure-mcp-integration.md)** - Guide for using clojure-mcp with this project
- **[docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md)** - Complete deployment guide for Cloudflare
- **[docs/security.md](./docs/security.md)** - Security controls & hardening guide
- **[docs/api.md](./docs/api.md)** - REST API reference
- **[docs/architecture.md](./docs/architecture.md)** - System architecture & design
- Component READMEs:
  - [components/game/README.md](./components/game/README.md) - Game engine internals
  - [components/user/README.md](./components/user/README.md) - User management & DB schema

## 🤝 Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

**Key principles**:
- Follow Polylith architecture (no cross-component `impl` dependencies)
- Use `clojure -M:poly check` before committing
- Write tests for new components (see existing tests for patterns)
- Use `brepl balance <file>` after editing Clojure files

## 🐛 Troubleshooting

**Database errors on startup**:
- Delete `simplicity.db` and restart (will reinitialize)

**Port already in use**:
- Set `PORT` environment variable: `PORT=3001 clojure -M -m cc.mindward.web-server.core`

**REPL won't connect**:
- Ensure Java 17+ is active (`java -version`)
- Check `.nrepl-port` file exists after `./bin/launchpad`

**Tests failing**:
- Run `clojure -M:poly check` to verify workspace integrity
- Ensure all deps downloaded: `clojure -A:dev:test -P`

## 📖 Learn More

- [Polylith Documentation](https://polylith.gitbook.io/)
- [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life)
- [Ring](https://github.com/ring-clojure/ring) - Clojure web applications
- [Hiccup](https://github.com/weavejester/hiccup) - HTML templating
- [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API) - Browser synthesis

---
*Built with simplicity and truth* • [GitHub](https://github.com/davidwuchn/simplicity)
