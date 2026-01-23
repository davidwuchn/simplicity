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
- [Babashka](https://babashka.org/) (optional, for scripts)

### Running the Application

1. **Clone and setup**:
   ```bash
   git clone <repository-url>
   cd simplicity
   clojure -A:dev -P  # Download dependencies
   ```

2. **Initialize database**:
   ```bash
   # Database schema is auto-created on first run
   # Location: ./simplicity.db (SQLite)
   ```

3. **Start the web server**:
   ```bash
   clojure -M -m cc.mindward.web-server.core
   ```

4. **Open in browser**:
   ```
   http://localhost:3000
   ```

5. **Create an account** and start playing!

### What's Running

- **Web Server**: http://localhost:3000 (configurable via `PORT` env var)
- **Database**: SQLite at `./simplicity.db`
- **Static Assets**: Served from `bases/web-server/resources/public/`

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

| Task | Command |
| :--- | :--- |
| **Check Workspace** | `clojure -M:poly check` |
| **Workspace Info** | `clojure -M:poly info` |
| **Run All Tests** | `clojure -M:poly test :dev` |
| **Run Component Tests** | `clojure -M:poly test brick:game` |
| **Lint Code** | `clj-kondo --lint components/*/src bases/*/src` |
| **Launch REPL** | `./bin/launchpad` (requires Java 17) |
| **Start nREPL** | `clojure -M:nrepl` (for clojure-mcp integration) |
| **Format Code** | `cljfmt fix` (if configured) |

### Development Workflow

The workspace supports **REPL-driven development** and **AI-assisted coding**:

1. Start REPL: `./bin/launchpad`
2. Connect your editor (nREPL port in `.nrepl-port`)
3. Reload components: `(require '[cc.mindward.game.interface :as game] :reload)`
4. Test functions interactively

Hot-reload is supported for most components.

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

**Current test coverage**: 501 passing assertions across 71 test cases

| Component | Tests | Assertions |
| :--- | :---: | :---: |
| Auth | 2 | 14 |
| Game | 15 | 146 |
| UI | 42 | 149 |
| User | 12 | 49 |
| Web-server | 28 | 143 |

Run tests:
```bash
# All tests
clojure -M:poly test :dev

# Specific component
clojure -M:poly test brick:game
clojure -M:poly test brick:user

# Security tests
clojure -M:poly test brick:web-server
```

Test files follow Polylith conventions:
- `components/<name>/test/cc/mindward/<name>/<namespace>_test.clj`
- Use fixtures for database isolation (see `user/interface_test.clj`)

**Security Testing:**
- SQL injection prevention
- XSS escaping verification
- CSRF protection
- Session security
- Rate limiting
- Input validation

See [docs/security.md](./docs/security.md) for complete security documentation.

## 🚢 Deployment

### Quick Deploy

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
