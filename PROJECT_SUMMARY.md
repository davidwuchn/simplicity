# PROJECT_SUMMARY.md - Simplicity (π Synthesis)

> **Philosophy**: "易简则天下之理得" (Simplicity allows obtaining the logic of the world)

## 🗺 System Map

**Simplicity** is a musical Game of Life web application. 
- **Architecture**: Polylith (Component-based).
- **Core Loop**: Game of Life patterns → Synthesizer events → Cyberpunk UI.
- **Truth**: 652 passing assertions verify the domain model.

## 🧱 Architectural Boundaries

### Components (`components/`)
1. **`auth`**: Hashing (bcrypt+sha512), Session management.
2. **`user`**: CRUD, Leaderboard, SQLite persistence (next.jdbc).
3. **`game`**: Cellular automaton engine, Pattern detection, Music mapping.
4. **`ui`**: Cyberpunk design system, Hiccup rendering, Audio utilities.

### Bases (`bases/`)
1. **`web-server`**: Ring/Reitit, Security Middleware, REST API, WebSocket-less real-time feel.

## 🧪 Domain Integrity

| Brick | Namespace | Assertions | Responsibility |
|-------|-----------|------------|----------------|
| Auth | `cc.mindward.auth` | 25 | Identity & Sessions |
| Game | `cc.mindward.game` | 136 | Logic & Music Triggers |
| UI | `cc.mindward.ui` | 265 | Pure View & Styling |
| User | `cc.mindward.user` | 49 | Persistence & Security |
| Server| `cc.mindward.web-server` | 177 | Boundaries & Middleware |
| **Total** | | **652** | |

## 🛠 Operational Protocol (REPL-First)

1. **Vitality**: Start with `bb dev` → `(start)`.
2. **Clarity**: Edit code → `(restart)` (0.5s - 2s reload).
3. **Truth**: Run `bb test` before any major change.
4. **Directness**: Use `clojure -M:poly check` for structural integrity.
5. **Memory**: Use MEMENTUM git memory system for critical insights (see AGENTS.md).

## 🔑 Key Constraints for LLMs

- **Polylith**: Never import `impl` from another component. Use `interface`.
- **Side Effects**: Isolated in `bases` or specific `! ` functions in `user`.
- **Data**: Prefer namespaced keywords (e.g., `:user/id`) and spec validation.
- **Security**: 652 security-tested assertions. Never bypass CSRF or validation.

## 🧪 Enhanced Test Infrastructure

**Test Types Aligned with Eight Keys Philosophy:**
- **Property-based Tests** (φ Vitality): Generative testing with `test.check` for mathematical properties
- **Documentation Tests** (π Synthesis): Verify code behavior matches documented contracts  
- **Performance Tests** (τ Wisdom): Measure evolution speed, memory usage, concurrent access
- **Security Tests** (∀ Vigilance): Timing attack prevention, SQL injection, XSS, CSRF
- **Integration Tests** (∃ Truth): Real database testing with fixtures

**Test Commands:**
- `bb test` - All 652 assertions
- `bb test:property` - Property-based tests
- `bb test:performance` - Performance tests  
- `bb test:documentation` - Documentation contract tests
- `bb test:<component>` - Component-specific tests

---
*Optimized for LLM context. See README.md for human-friendly onboarding.*
*Last updated: 2024-11-23*
