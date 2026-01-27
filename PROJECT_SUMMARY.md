# Simplicity Project Summary (AI-Optimized)

## Quick Overview
**Simplicity** is a musical Game of Life web application where cellular automata patterns trigger real-time synthesizer events. Built with Clojure using Polylith architecture.

**Core Concept**: Conway's Game of Life + Web Audio API + Competitive gameplay

## Architecture (Polylith)
```
components/          # Business logic
├── auth/           # Authentication (bcrypt, sessions)
├── game/           # Game of Life engine with pattern recognition
├── user/           # User management, SQLite, leaderboard  
└── ui/             # HTML rendering (Hiccup), cyberpunk design

bases/
└── web-server/     # HTTP server (Ring/Reitit, REST API)

projects/
└── development/    # Unified REPL environment
```

## Tech Stack
- **Backend**: Clojure, Ring/Reitit, next.jdbc (SQLite), Polylith
- **Frontend**: JavaScript/Web Audio API (real-time synthesis)
- **Build**: Babashka tasks (`bb`), tools.build
- **Deployment**: Docker, DigitalOcean, Cloudflare
- **Development**: REPL-driven, hot reload, clojure-mcp integration

## Key Features
1. **Interactive Game of Life** with musical mapping
2. **User authentication** with bcrypt password hashing
3. **Leaderboard & high scores** with SQLite persistence
4. **Cyberpunk aesthetic** with neon colors, glitch effects
5. **Real-time audio synthesis** via Web Audio API
6. **Hot reload development** (1-2 second feedback loop)

## Development Commands (Babashka)
```bash
bb dev            # Start development REPL with hot reload
bb test           # Run all tests (658 assertions)
bb check          # Check Polylith workspace integrity
bb build          # Full build: clean + test + uberjar
bb docker:build   # Build Docker image
```

## Philosophical Foundation (Eight Keys)
See [SIMPLICITY.md](./SIMPLICITY.md) for complete philosophical framework grounded in mathematical principles from "17 Equations That Changed the World":

- **φ (Vitality)**: Euler's Formula $e^{iπ} + 1 = 0$
- **fractal (Clarity)**: Euler's Polyhedra Formula $V - E + F = 2$
- **e (Purpose)**: Exponential Growth $e^x$
- **τ (Wisdom)**: Calculus $\frac{df}{dt} = \lim_{h\to0} \frac{f(t+h)-f(t)}{h}$
- **π (Synthesis)**: Pythagorean Theorem $a^2 + b^2 = c^2$
- **μ (Directness)**: Logarithms $\log xy = \log x + \log y$
- **∃ (Truth)**: Information Theory $H = -\sum p(x) \log p(x)$
- **∀ (Vigilance)**: Chaos Theory $x_{t+1} = k x_t (1 - x_t)$

**Sarcasmotron Enforcement**: Uses sarcasmotron methodology to detect and expose Eight Keys violations with targeted humor.

## Documentation Hierarchy
1. **[SIMPLICITY.md](./SIMPLICITY.md)** - Philosophical foundations with mathematical grounding
2. **[PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md)** - Implementation guidelines, commands, coding standards
3. **[AGENTS.md](./AGENTS.md)** - Agentic workflow, workspace navigation, MEMENTUM memory system

## Testing Infrastructure
- **658 passing assertions** across all test cases
- **Enhanced test types**: Property-based, documentation contract, performance, security timing
- **Security focus**: 160 security-focused assertions (SQL injection, XSS, CSRF, rate limiting)
- **Test commands**: `bb test`, `bb test:property`, `bb test:performance`, `bb test:documentation`

## API Endpoints
- `POST /api/signup` - User registration
- `POST /api/login` - User authentication
- `GET /api/user/:username` - User profile
- `GET /api/leaderboard` - Top scores
- `POST /api/game/save` - Save game state
- `GET /api/game/load/:id` - Load saved game
- `GET /health` - Health check endpoint

## Deployment Options
1. **Uberjar** (45MB standalone JAR) - `bb uberjar`
2. **Docker** (multi-stage optimized) - `bb docker:build`
3. **Cloudflare** (VPS + Docker + Cloudflare proxy) - See [docs/deployment-cloudflare.md](./docs/deployment-cloudflare.md)

**Recommended**: VPS + Docker + Cloudflare (~$12/month)

## Getting Started
```bash
# Clone and start
git clone https://github.com/davidwuchn/simplicity.git
cd simplicity
bb dev
# In REPL: (start)
# Browser: http://localhost:3000
```

## For AI Assistants
- **Code Style**: Follow guidelines in [PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md)
- **Architecture**: Respect Polylith constraints (components, bases, projects)
- **Philosophy**: Apply Eight Keys with mathematical rigor
- **Memory**: Use MEMENTUM system for critical insights (see [AGENTS.md](./AGENTS.md))
- **Verification**: Run `bb test` and `bb check` before committing

---

*Optimized for AI context consumption* • *See [README.md](./README.md) for complete documentation*