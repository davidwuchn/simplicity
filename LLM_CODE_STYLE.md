# LLM Code Style Guide - Simplicity (Polylith)

> **Philosophy**: "易简则天下之理得" (Simplicity allows obtaining the logic of the world)

This document defines the coding standards for the Simplicity project when working with AI assistants. Follow these guidelines to maintain consistency and quality across the codebase.

## Core Principles (Eight Keys)

- **φ (Vitality)**: Prioritize organic, non-repetitive generation
- **fractal (Clarity)**: Filter ambiguity; demand objective precision
- **e (Purpose)**: Goal-oriented; require specific, actionable function
- **τ (Wisdom)**: Prioritize judgment and foresight over raw speed
- **π (Synthesis)**: Demand complete mental models and holistic integration
- **μ (Directness)**: Cut pleasantries and bias to reach raw reality
- **∃ (Truth)**: Metric of truth as "unconcealment"; favor underlying reality
- **∀ (Vigilance)**: Defensive constraint against fallacies and manipulative framing

## Polylith Architecture Constraints

### Components
- **Encapsulation**: All business logic resides in `components/`
- **Interface**: The `interface` namespace is the ONLY public entry point
- **Implementation**: The `impl` namespace is private - never refer to `impl` from outside
- **Naming**: `cc.mindward.component.<name>.interface`

### Bases
- **Role**: Entry points only (REST APIs, CLI, Lambda)
- **Delegation**: Bases must NOT contain business logic - delegate to components
- **Rendering**: Bases must NOT generate UI - delegate to a `ui` component
- **Naming**: `cc.mindward.base.<name>.core`

### Projects
- **Artifacts**: Define how components and bases compose into deployable artifacts
- **Configuration**: Managed via `deps.edn` in `projects/` directory

## Code Style

### Namespaces & Imports
```clojure
(ns cc.mindward.component.example.interface
  (:require [clojure.string :as str]
            [cc.mindward.component.other.interface :as other]))
```

- Use kebab-case for namespaces and files
- Root everything under `cc.mindward`
- Avoid `:use`, `:refer :all`, and naked symbols in `:require`
- Order: Standard library first, then external libraries, then internal bricks

### Formatting
- **Indentation**: 2-space indentation, no tabs
- **Line Length**: Max 100 characters
- **Brackets**: Avoid dangling brackets, use Lisp style (e.g., `))`)
- **Whitespace**: Single empty line between top-level forms, no trailing whitespace

### Naming Conventions
- **Variables/Functions**: `kebab-case`
- **Predicates**: End with `?` (e.g., `authorized?`, `empty?`)
- **Side Effects**: End with `!` (e.g., `save!`, `delete-user!`)
- **Anaphoric**: Use `_` for unused bindings

### Error Handling
```clojure
(throw (ex-info "Failed to process order" 
                {:order-id 123 :reason :insufficient-funds}))
```

- Use `ex-info` with descriptive message and context map
- Catch exceptions at the Base level (public API) and log/report

### Types & Data
- Use `clojure.spec.alpha` to define domain constraints in `interface` namespaces
- Prefer maps over multiple positional arguments
- Use namespaced keywords for domain entities (e.g., `:user/id`)

## Security Constraints

- **CSRF Protection**: All state-changing endpoints require CSRF tokens
- **Input Validation**: All user inputs must be validated at the boundary
- **SQL Injection**: Use parameterized queries only
- **XSS Prevention**: HTML escaping + CSP headers
- **Password Security**: bcrypt + sha512 with timing attack resistance
- **Rate Limiting**: Token bucket algorithm for sensitive endpoints

## Anti-Patterns to Avoid

❌ **Complexity**: Functions exceeding 20 lines (reconsider domain model)
❌ **Dependency Hell**: Circular dependencies between components
❌ **Code Slop**: Commented-out code or `println` in production paths
❌ **Abstraction Leak**: Implementation details escaping component interfaces
❌ **The God Base**: Mixing routing, logic, and HTML in one file
❌ **The Test Illusion**: `(is (= 1 1))` is not a test - verify actual logic
❌ **Hardcoded Secrets**: Never commit passwords or API keys

## Testing

- **Test Coverage**: Maintain current coverage (501 assertions across 71 tests)
- **Stateful Testing**: Use `use-fixtures` with temporary SQLite files
- **Isolation**: Use `with-redefs` to isolate component logic
- **No Global State**: Never rely on shared global state

## REPL-Driven Development

- Always work from the `development` project for cross-component REPL access
- Use the **hot reload workflow** for instant feedback:
  ```clojure
  user=> (start)    ; Start server
  ;; Edit code...
  user=> (restart)  ; Hot reload (1-2 seconds!)
  ```
- Use `brepl balance <file>` after every edit to ensure structural integrity
- Use heredoc pattern for evaluation: `brepl <<'EOF' ... EOF`
- Always use `:reload` when requiring namespaces to pick up changes
- See [docs/hot-reload-workflow.md](../docs/hot-reload-workflow.md) for complete guide

## Verification Workflow

1. **Check Structure**: Run `brepl balance <file>` after edits
2. **Hot Reload**: Run `(restart)` in REPL to apply changes
3. **Test**: Verify at http://localhost:3000
4. **Lint**: Run `clj-kondo --lint src`
5. **Test Suite**: Run `clojure -M:poly test :dev`
6. **Verify Polylith**: Run `clojure -M:poly check`

## Quick Reference Commands

```bash
# Start development with hot reload
bb dev

# In REPL:
(start)       # Start server
(restart)     # Hot reload (main workflow)
(stop)        # Stop server
(status)      # Check status
(help)        # Show commands

# Check workspace
clojure -M:poly check

# Run all tests
clojure -M:poly test :dev

# Start nREPL (for clojure-mcp)
clojure -M:nrepl

# Check formatting
brepl balance <file> --dry-run
```

---
*This style guide ensures AI-assisted code maintains the same quality standards as human-written code.*
