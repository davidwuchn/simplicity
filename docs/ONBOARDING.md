# Developer Onboarding Guide

Welcome to the Simplicity project! This guide will help you get up and running quickly.

## 🎯 Quick Start (5 minutes)

### Prerequisites
- **Java 17+** (check: `java -version`)
- **Clojure CLI** (install: https://clojure.org/guides/install_clojure)
- **Git**

### Setup
```bash
# 1. Clone the repository
git clone https://github.com/YOUR_ORG/simplicity.git
cd simplicity

# 2. Install dependencies
clojure -P -M:dev:test:poly

# 3. Run tests (verify setup)
clojure -M:poly test :dev

# 4. Start REPL
clojure -A:dev:poly

# 5. In REPL:
(require '[user :as u])
(u/start!)  ;; Starts web server on http://localhost:3000

# 6. Open browser
open http://localhost:3000
```

✅ **You're ready!** If tests pass and you see the landing page, you're all set.

---

## 📚 Understanding the Codebase

### Architecture: Polylith

This project uses **Polylith**, which organizes code into reusable components.

```
simplicity/
├── components/          # Business logic (reusable)
│   ├── auth/           # Authentication
│   ├── game/           # Game of Life engine
│   ├── user/           # User management
│   └── ui/             # HTML generation
├── bases/              # Entry points
│   └── web-server/     # HTTP server
├── projects/
│   └── development/    # REPL-driven dev
└── docs/               # Documentation
```

**Key Concept**: Components have `interface` (public API) and `impl` (private implementation).

```clojure
;; Good: Use interface
(require '[cc.mindward.user.interface :as user])
(user/create-user! {...})

;; Bad: Never use impl from outside component
(require '[cc.mindward.user.impl :as impl]) ;; ❌ Polylith violation
```

**Verify architecture**:
```bash
clojure -M:poly check  # Must pass!
```

### Component Overview

| Component | Purpose | Key Functions |
|-----------|---------|---------------|
| **auth** | Authentication | `authenticate` |
| **game** | Game of Life engine | `create-game!`, `evolve!` |
| **user** | User management | `create-user!`, `get-leaderboard` |
| **ui** | HTML rendering | `landing-page`, `game-page` |
| **web-server** (base) | HTTP routing | Ring handlers |

---

## 🔧 Development Workflow

### 1. REPL-Driven Development

**Start REPL:**
```bash
clojure -A:dev:poly
```

**Common REPL commands:**
```clojure
;; Start server
(require '[user :as u])
(u/start!)

;; Stop server
(u/stop!)

;; Restart (reload code)
(u/restart!)

;; Run all tests
(u/test-all)

;; Run specific component tests
(require '[cc.mindward.game.interface-test])
(clojure.test/run-tests 'cc.mindward.game.interface-test)
```

### 2. Running Tests

```bash
# All tests
clojure -M:poly test :dev

# Specific component
clojure -M:poly test brick:user

# With coverage (coming soon)
clojure -M:test:cov
```

### 3. Code Style

**Follow AGENTS.md guidelines:**
- Use `kebab-case` for names
- Predicates end with `?` (e.g., `valid?`)
- Side effects end with `!` (e.g., `create-user!`)
- Max 100 chars per line
- 2-space indentation

**Run linter:**
```bash
clj-kondo --lint components bases
```

### 4. Making Changes

**Workflow:**
1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes
3. Run tests: `clojure -M:poly test :dev`
4. Check architecture: `clojure -M:poly check`
5. Lint: `clj-kondo --lint components bases`
6. Commit: `git commit -m "Add feature X"`
7. Push and create PR

**Pre-commit hooks** (auto-installed):
- Runs `poly check`
- Runs linter
- Runs tests

---

## 🧭 Finding Your Way Around

### Where to Find Things

| Task | Location |
|------|----------|
| Add new route | `bases/web-server/src/cc/mindward/web_server/core.clj` |
| Change game rules | `components/game/src/cc/mindward/game/impl.clj` |
| Modify UI | `components/ui/src/cc/mindward/ui/` |
| Add security | `bases/web-server/src/cc/mindward/web_server/security.clj` |
| Database schema | `components/user/src/cc/mindward/user/impl.clj` (init-db!) |
| Configuration | `deps.edn` (root and component-level) |

### Key Files

- **AGENTS.md**: Architecture rules and philosophy
- **docs/architecture/**: Architecture Decision Records (ADRs)
- **docs/deployment-cloudflare.md**: Deployment guide
- **docs/security.md**: Security documentation
- **build.clj**: Build configuration

---

## 🐛 Debugging

### Common Issues

**1. "Unresolved namespace"**
```
Solution: Add require to deps.edn or ns form
Check: lein check or clj-kondo
```

**2. "Polylith check fails"**
```
Error: Circular dependency or illegal import
Solution: Review component dependencies
Tool: clojure -M:poly info
```

**3. "Tests fail after pull"**
```
Solution: Clean and reinstall deps
Commands:
  rm -rf .cpcache target
  clojure -P -M:dev:test:poly
  clojure -M:poly test :dev
```

**4. "Port 3000 already in use"**
```
Solution: Kill existing process or use different port
Commands:
  lsof -ti:3000 | xargs kill -9
  # or
  PORT=3001 clojure -A:dev:poly
```

### Debugging in REPL

```clojure
;; Enable debug logging
(require '[clojure.tools.logging :as log])
(System/setProperty "LOG_LEVEL" "DEBUG")

;; Inspect database
(require '[cc.mindward.user.impl :as user-impl])
(user-impl/get-leaderboard)

;; Check game state
(require '[cc.mindward.game.impl :as game-impl])
@game-impl/games  ;; View all active games

;; Test SQL queries
(require '[next.jdbc :as jdbc])
(jdbc/execute! user-impl/*ds* ["SELECT * FROM users"])
```

---

## 📖 Learning Resources

### Clojure
- [Clojure for the Brave and True](https://www.braveclojure.com/)
- [ClojureDocs](https://clojuredocs.org/)
- [Clojure Style Guide](https://guide.clojure.style/)

### Polylith
- [Polylith Documentation](https://polylith.gitbook.io/)
- [Polylith GitHub](https://github.com/polyfy/polylith)
- [Architecture Overview](./architecture/001-polylith-architecture.md)

### Project-Specific
- [AGENTS.md](../AGENTS.md) - **READ THIS FIRST**
- [Architecture Decision Records](./architecture/)
- [Security Guide](./security.md)
- [Deployment Guide](./deployment-cloudflare.md)

---

## 🤝 Contributing

### Code Review Checklist

Before submitting a PR, ensure:
- [ ] Tests pass (`clojure -M:poly test :dev`)
- [ ] Polylith check passes (`clojure -M:poly check`)
- [ ] Linter passes (`clj-kondo --lint components bases`)
- [ ] Code follows AGENTS.md guidelines
- [ ] New functions have docstrings
- [ ] Security implications considered (if touching auth/user data)
- [ ] ADR created for architectural changes

### Getting Help

- **Questions**: Open GitHub Discussion
- **Bugs**: File GitHub Issue
- **Security**: Email security@example.com (private disclosure)

---

## 🚀 Next Steps

1. **Read [AGENTS.md](../AGENTS.md)** - Core philosophy and rules
2. **Explore codebase**: Start with `components/game/` (simple, well-documented)
3. **Make a change**: Add a feature or fix a bug (check Issues for "good first issue")
4. **Run the full test suite**: `clojure -M:poly test :dev`
5. **Deploy locally**: Try building a Docker image

**Pro Tips:**
- Use `poly info` to visualize dependencies
- Run `poly check` frequently during development
- Keep the REPL running - reload code instead of restarting
- Read the ADRs to understand "why" decisions were made

---

**Welcome to the team! 🎉**

If you have questions, don't hesitate to ask. We value curiosity and learning.

---

*Last Updated: 2026-01-23*
