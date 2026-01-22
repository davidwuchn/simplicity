# Contributing to Simplicity

Thank you for your interest in contributing to Simplicity! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Project Structure](#project-structure)

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inspiring community for all. Please be respectful and constructive in all interactions.

### Expected Behavior

- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

- **Java 17+** ([Download](https://adoptium.net/))
- **Clojure CLI** 1.11+ ([Install Guide](https://clojure.org/guides/install_clojure))
- **Git** ([Install Guide](https://git-scm.com/downloads))
- **Docker** (optional, for containerized development)

### Fork and Clone

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/simplicity.git
   cd simplicity
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/davidwuchn/simplicity.git
   ```

### Install Dependencies

```bash
clojure -A:dev -P  # Download all dependencies
```

### Verify Setup

```bash
# Check workspace integrity
clojure -M:poly check

# Run all tests
clojure -M:poly test :dev

# Start development server
clojure -M -m cc.mindward.web-server.core
```

Open http://localhost:3000 to verify the application runs.

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/my-new-feature
# or
git checkout -b fix/bug-description
```

**Branch Naming**:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `test/` - Test improvements
- `chore/` - Maintenance tasks

### 2. Make Changes

Follow the [Polylith architecture](https://polylith.gitbook.io/) principles:

- **Components** (`components/`) - Business logic only
- **Bases** (`bases/`) - Entry points, no business logic
- **Tests** - Co-located with components/bases

**Key Rules**:
- ✅ Components depend on other component **interfaces** only
- ❌ Never import from `impl` namespaces outside the component
- ✅ Keep functions small (< 20 lines)
- ✅ Use pure functions where possible

### 3. Run Tests

```bash
# Run all tests
clojure -M:poly test :dev

# Run specific component tests
clojure -M:poly test brick:game
clojure -M:poly test brick:user

# Check workspace integrity
clojure -M:poly check
```

### 4. Lint Code

```bash
clj-kondo --lint components/*/src bases/*/src
```

### 5. Balance Clojure Files

After editing Clojure files, ensure structural integrity:

```bash
brepl balance <file>.clj
```

## Coding Standards

### Clojure Style

Follow the style guide in [AGENTS.md](./AGENTS.md):

**Naming**:
- `kebab-case` for functions and variables
- `PascalCase` for records/protocols
- Predicates end with `?` (e.g., `valid?`)
- Side effects end with `!` (e.g., `save!`)

**Formatting**:
- 2-space indentation
- Max 100 characters per line
- No trailing whitespace
- Single empty line between top-level forms

**Example**:
```clojure
(ns cc.mindward.component.example.interface
  (:require [clojure.string :as str]))

(defn process-data
  "Process input data and return result."
  [data]
  (-> data
      (str/trim)
      (str/upper-case)))

(defn valid-input?
  "Check if input is valid."
  [input]
  (and (string? input)
       (not (str/blank? input))))
```

### Component Structure

```
components/my-component/
├── src/cc/mindward/my_component/
│   ├── interface.clj   # PUBLIC API (only this is imported by others)
│   ├── impl.clj        # Private implementation
│   └── spec.clj        # clojure.spec definitions (optional)
├── test/cc/mindward/my_component/
│   └── interface_test.clj
└── deps.edn            # Component-specific dependencies
```

### Security Guidelines

- ✅ Always use parameterized queries (never string concatenation)
- ✅ Validate all user input at API boundaries
- ✅ Use bcrypt for password hashing
- ✅ Include CSRF tokens on all state-changing endpoints
- ✅ Escape HTML output (Hiccup does this automatically)
- ❌ Never commit secrets or API keys
- ❌ Never log sensitive data (passwords, tokens)

See [docs/security.md](./docs/security.md) for complete guidelines.

## Testing Guidelines

### Test Coverage Goals

- **Unit Tests**: All public interface functions
- **Integration Tests**: HTTP endpoints, database operations
- **Security Tests**: SQL injection, XSS, CSRF protection

### Writing Tests

```clojure
(ns cc.mindward.component.example.interface-test
  (:require [clojure.test :refer [deftest testing is]]
            [cc.mindward.component.example.interface :as example]))

(deftest process-data-test
  (testing "processes valid input"
    (is (= "HELLO" (example/process-data "  hello  "))))
  
  (testing "handles nil input"
    (is (nil? (example/process-data nil)))))
```

### Database Testing

Use temporary databases for isolation:

```clojure
(use-fixtures :each
  (fn [f]
    (let [temp-db (create-temp-db!)]
      (with-redefs [db/datasource (atom temp-db)]
        (f)
        (delete-temp-db! temp-db)))))
```

### Running Tests

```bash
# All tests
clojure -M:poly test :dev

# Watch mode (auto-run on file changes)
clojure -M:dev:test -X kaocha.runner/exec-fn

# With coverage
clojure -M:dev:test:coverage
```

## Commit Guidelines

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation changes
- `refactor` - Code refactoring
- `test` - Test improvements
- `chore` - Maintenance tasks
- `perf` - Performance improvements
- `security` - Security improvements

**Example**:
```
feat(game): add glider gun pattern recognition

- Detect Gosper glider gun pattern
- Add test coverage for pattern detection
- Update musical triggers for complex patterns

Closes #42
```

### Commit Best Practices

- ✅ One logical change per commit
- ✅ Write clear, descriptive messages
- ✅ Reference issues (e.g., "Fixes #123")
- ❌ Don't commit commented-out code
- ❌ Don't commit `println` debugging statements
- ❌ Don't commit broken tests

## Pull Request Process

### Before Submitting

1. **Sync with upstream**:
   ```bash
   git fetch upstream
   git rebase upstream/master
   ```

2. **Run all tests**:
   ```bash
   clojure -M:poly test :dev
   ```

3. **Check workspace integrity**:
   ```bash
   clojure -M:poly check
   ```

4. **Lint code**:
   ```bash
   clj-kondo --lint components/*/src bases/*/src
   ```

5. **Balance Clojure files**:
   ```bash
   brepl balance <changed-files>.clj
   ```

### Submitting

1. **Push to your fork**:
   ```bash
   git push origin feature/my-new-feature
   ```

2. **Create Pull Request** on GitHub

3. **Fill out PR template** with:
   - Description of changes
   - Related issues
   - Testing performed
   - Screenshots (if UI changes)

### PR Title Format

```
<type>: <description>
```

Examples:
- `feat: add WebSocket support for real-time updates`
- `fix: resolve race condition in game state updates`
- `docs: improve API documentation with examples`

### Review Process

1. **Automated Checks** (CI):
   - All tests pass
   - Code linting passes
   - Polylith workspace check passes

2. **Code Review**:
   - At least one maintainer approval required
   - Address all review comments
   - Keep discussion professional and constructive

3. **Merge**:
   - Squash and merge (for feature branches)
   - Rebase and merge (for small fixes)

## Project Structure

### Polylith Architecture

```
simplicity/
├── components/          # Business logic (reusable)
│   ├── auth/           # Authentication
│   ├── game/           # Game of Life engine
│   ├── user/           # User management
│   └── ui/             # HTML rendering
├── bases/              # Entry points (not reusable)
│   └── web-server/     # HTTP server
├── development/        # Unified REPL environment
├── docs/               # Documentation
├── scripts/            # Build and utility scripts
└── tests.edn           # Test configuration
```

### Adding a New Component

1. **Create component structure**:
   ```bash
   mkdir -p components/my-component/src/cc/mindward/my_component
   mkdir -p components/my-component/test/cc/mindward/my_component
   ```

2. **Create interface**:
   ```clojure
   ;; components/my-component/src/cc/mindward/my_component/interface.clj
   (ns cc.mindward.my-component.interface
     (:require [cc.mindward.my-component.impl :as impl]))
   
   (defn do-something [x]
     (impl/do-something x))
   ```

3. **Register in `deps.edn`**:
   ```clojure
   :dev {:extra-paths ["components/my-component/src"
                       "components/my-component/test"]}
   ```

4. **Verify**:
   ```bash
   clojure -M:poly check
   ```

## Getting Help

- **Documentation**: Start with [README.md](./README.md) and [AGENTS.md](./AGENTS.md)
- **Issues**: Search [existing issues](https://github.com/davidwuchn/simplicity/issues)
- **Discussions**: Use GitHub Discussions for questions
- **Architecture**: See [docs/architecture.md](./docs/architecture.md)
- **API**: See [docs/api.md](./docs/api.md)

## Recognition

Contributors will be recognized in:
- GitHub contributors page
- CHANGELOG.md for significant contributions
- Special thanks in release notes

Thank you for contributing to Simplicity! 🎮🎵

---

*Built with simplicity and truth*
