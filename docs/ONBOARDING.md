# Developer Onboarding Guide

Welcome to the Simplicity project! This guide will help you get up and running quickly.

## 🎯 Quick Start (5 minutes)

### Prerequisites
- **Java 17+** (check: `java -version`)
- **[Babashka](https://babashka.org/)** (essential task runner)
- **Clojure CLI** (install: https://clojure.org/guides/install_clojure)
- **Git**

### Setup
```bash
# 1. Clone the repository
git clone https://github.com/davidwuchn/simplicity.git
cd simplicity

# 2. Start development environment
bb dev

# 3. In REPL, start the server:
user=> (start)

# 4. Open browser
# http://localhost:3000
```

✅ **You're ready!** If the server starts and you see the landing page, you're all set.

---

## 🧱 Architecture: Polylith

This project uses **Polylith**, which organizes code into reusable components.

```
simplicity/
├── components/          # Business logic (reusable)
│   ├── auth/           # Authentication
│   ├── game/           # Game of Life engine
│   ├── user/           # User management
│   └── ui/             # HTML generation (Hiccup)
├── bases/              # Entry points (REST APIs)
│   └── web-server/     # HTTP server (Ring/Reitit)
├── projects/
│   └── development/    # REPL-driven dev
└── docs/               # Documentation
```

**Key Concept**: Components have `interface` (public API) and `impl` (private implementation). Never refer to `impl` from outside a component.

**Verify architecture**:
```bash
bb check
```

---

## 🔧 Development Workflow

### 1. REPL-Driven Development (Hot Reload)

**Primary Workflow:**
1. Run `bb dev` to start the REPL.
2. Run `(start)` to start the server.
3. Edit code in your editor.
4. Run `(restart)` in the REPL to apply changes instantly (~0.5s).

See **[hot-reload.md](./hot-reload.md)** for more details.

### 2. Running Tests
```bash
bb test              # Run all tests (~736 assertions)
bb test:watch        # Watch mode (auto-rerun on changes)
bb test:game         # Run game component tests
bb test:user         # Run user component tests
bb test:auth         # Run auth component tests
bb test:ui           # Run UI component tests
bb test:web-server   # Run web-server base tests
bb test:property     # Run property-based tests (test.check)
bb test:performance  # Run performance tests
bb test:documentation # Run documentation contract tests
```

### 3. Code Style & Philosophy
Follow the **Eight Keys** in [SIMPLICITY.md](../SIMPLICITY.md):
- **φ (Vitality)**, **fractal (Clarity)**, **π (Synthesis)**, etc.

Follow coding standards in [PRACTICAL_GUIDE.md](../PRACTICAL_GUIDE.md):
- Use `kebab-case` for names.
- Predicates end with `?` (e.g., `valid?`).
- Side effects end with `!` (e.g., `save!`).

---

## 🧭 Finding Your Way Around

| Task | Location |
|------|----------|
| Add new route | `bases/web-server/src/cc/mindward/web_server/core.clj` |
| Change game rules | `components/game/src/cc/mindward/game/impl.clj` |
| Modify UI/Styles | `components/ui/src/cc/mindward/ui/` |
| Database schema | `components/user/src/cc/mindward/user/impl.clj` |
| Tooling/Tasks | `bb.edn` |

### Key Files
- **SIMPLICITY.md**: Philosophical foundations & Eight Keys
- **PRACTICAL_GUIDE.md**: Implementation guidelines & commands
- **AGENTS.md**: Agentic workflow & workspace navigation
- **PROJECT_SUMMARY.md**: System map (optimized for LLMs)
- **docs/hot-reload.md**: How to use the hot reload system
- **docs/security.md**: Security controls (160 security assertions)

---

## 📖 Learning Resources
- [Polylith Documentation](https://polylith.gitbook.io/)
- [Clojure REPL Guide](https://clojure.org/guides/repl/introduction)
- [AGENTS.md](../AGENTS.md) - **Workspace navigation guide (start here)**
- [SIMPLICITY.md](../SIMPLICITY.md) - Philosophical foundations & Eight Keys
- [PRACTICAL_GUIDE.md](../PRACTICAL_GUIDE.md) - Implementation guidelines

---

**Welcome to the team! 🎉**
*Philosophy: 易简则天下之理得 (Simplicity allows obtaining the logic of the world)*

---
*Last Updated: 2024-11-23*
