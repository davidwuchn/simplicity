# Contributing to Simplicity

Thank you for your interest in contributing to Simplicity! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Philosophy](#philosophy)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)

## Philosophy

**易简则天下之理得** (Simplicity allows obtaining the logic of the world)

We follow the **Eight Keys** (see [SIMPLICITY.md](./SIMPLICITY.md)):
- **φ Vitality**, **fractal Clarity**, **π Synthesis**, **μ Directness**, **∃ Truth**, etc.

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Babashka** (Essential task runner)
- **Clojure CLI** 1.11+
- **Git**

### Fork and Clone

1. **Fork the repository** on GitHub.
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/simplicity.git
   cd simplicity
   ```

### Verify Setup

```bash
bb check       # Check workspace integrity
bb test        # Run all tests (~736 assertions)
```

---

## Development Workflow

### 1. The REPL-First Workflow (Hot Reload)

We use a high-velocity development cycle with hot reload.

1. **Start REPL**: `bb dev`
2. **Start Server**: `(start)` in the REPL.
3. **Edit Code**: Make changes in `components/` or `bases/`.
4. **Hot Reload**: `(restart)` in the REPL (takes ~0.5s).
5. **Verify**: Test changes at http://localhost:3000.

See **[docs/hot-reload.md](./docs/hot-reload.md)** for a complete guide.

### 2. Making Changes

Follow the [Polylith architecture](https://polylith.gitbook.io/) principles:

- **Components** (`components/`) - Business logic only.
- **Bases** (`bases/`) - Entry points (APIs, CLI), no business logic.
- **Interfaces**: Only require `interface` namespaces from other components. Never require `impl`.

**Rules**:
- Keep functions small (< 20 lines).
- Use pure functions where possible.
- Define domain constraints with `clojure.spec`.

### 3. Validation

```bash
bb check       # Validate Polylith architecture
bb lint        # Lint all source files
bb test        # Ensure all tests pass (see test-stats.edn)
```

---

## Coding Standards

Follow the style guide in [PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md):

**Naming**:
- `kebab-case` for functions and variables.
- Predicates end with `?` (e.g., `valid?`).
- Side effects end with `!` (e.g., `save!`).

**Formatting**:
- 2-space indentation.
- Max 100 characters per line.
- Use `brepl balance <file>` to verify structural integrity after edits.

---

## Testing Guidelines

### Test Coverage Goals
- **Total Assertions**: 652 (Current truth).
- **Security**: 160 security-focused assertions.
- **UI**: 265 assertions verifying the cyberpunk interface.

### Running Tests
```bash
bb test              # Run everything
bb test:watch        # Auto-run on file changes
bb test:game         # Test specific component
```

---

## Commit Guidelines

### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `security`.

**Example**:
```
feat(game): add glider gun pattern recognition

- Detect Gosper glider gun pattern
- Add musical triggers for complex patterns
- ∃ Truth: 15 new assertions added

Closes #42
```

---

## Pull Request Process

### Before Submitting
1. **Sync with upstream**: `git fetch upstream && git rebase upstream/master`.
2. **Run Validation**: `bb check && bb lint && bb test`.
3. **Clean Up**: Ensure no `println` or commented-out code remains.

### Submitting
1. Push to your fork and create a PR on GitHub.
2. Fill out the PR template clearly.
3. Ensure all CI checks pass.

---
*Built with simplicity and truth*
*Last Updated: 2024-11-23*
