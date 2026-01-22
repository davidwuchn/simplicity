# Pre-commit Hooks

This directory contains Git pre-commit hooks for the Simplicity repository.

## Installation

### Automatic (Recommended)
```bash
# Install the pre-commit hook
./scripts/install-hooks.sh
```

### Manual
```bash
# Copy the hook to .git/hooks/
cp scripts/pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## What Gets Checked

The pre-commit hook runs the following checks:

### 1. Clojure/EDN File Balance ✓
- Checks all staged `.clj`, `.cljs`, `.cljc`, and `.edn` files
- Verifies parentheses, brackets, and braces are balanced
- **Requires**: `brepl` (optional, will skip if not installed)
- **Fix**: `brepl balance <file>`

### 2. Linting (clj-kondo) ⚠️
- Lints all Clojure code for common issues
- **Requires**: `clj-kondo` (optional, will skip if not installed)
- **Non-blocking**: Shows warnings but doesn't fail commit
- **Install**: `brew install borkdude/brew/clj-kondo`

### 3. Polylith Workspace Check ✓
- Validates Polylith architecture constraints
- Checks for circular dependencies
- Verifies component isolation
- **Blocking**: Commit fails if workspace is invalid
- **Fix**: `clojure -M:poly check` to see errors

### 4. Component Tests ✓
- Runs tests for changed components
- Ensures no regressions introduced
- **Blocking**: Commit fails if tests fail
- **Fix**: Fix failing tests before committing

### 5. Common Issues Check 🔒
- **Debug statements**: Warns about `println "DEBUG"` statements
- **Hardcoded secrets**: Blocks commits with hardcoded passwords/tokens
- **Large files**: Warns about files >1MB
- **Blocking**: Only secrets check blocks commit

## Usage

Once installed, the hook runs automatically on `git commit`:

```bash
# Make changes
vim components/game/src/cc/mindward/game/impl.clj

# Stage changes
git add components/game/src/cc/mindward/game/impl.clj

# Commit (hook runs automatically)
git commit -m "feat: add new game feature"

# Output:
# 🔍 Running pre-commit checks...
# 📝 Checking Clojure/EDN file balance...
# ✓ All files are balanced
# 🔎 Running clj-kondo linter...
# ✓ No linting issues found
# 🏗️  Checking Polylith workspace...
# ✓ Polylith workspace check passed
# 🧪 Running tests for changed code...
#   Testing component: game
# ✓ All component tests passed
# 🔒 Checking for common issues...
# ✓ All checks passed!
# ✅ Pre-commit checks complete. Proceeding with commit...
```

## Skipping Hooks (Not Recommended)

In rare cases where you need to skip the pre-commit hook:

```bash
git commit --no-verify -m "WIP: work in progress"
```

**Warning**: Only use `--no-verify` for temporary commits that will be squashed later. Never push commits that skip hooks to `main`/`master`.

## Troubleshooting

### Hook Not Running
```bash
# Verify hook is installed
ls -la .git/hooks/pre-commit

# Should show executable permissions (-rwxr-xr-x)
# If not:
chmod +x .git/hooks/pre-commit
```

### Balance Check Failing
```bash
# Run brepl balance to see what's wrong
brepl balance <file> --dry-run

# Auto-fix
brepl balance <file>

# Stage fixed file
git add <file>
```

### Polylith Check Failing
```bash
# See detailed error
clojure -M:poly check

# Common fixes:
# - Remove circular dependencies
# - Don't import impl namespace from outside component
# - Add missing brick to deps.edn :dev alias
```

### Tests Failing
```bash
# Run tests manually to see full output
clojure -M:poly test brick:<component-name>

# Fix the failing tests
# Stage changes
git add .
```

### Hook Too Slow
If the pre-commit hook is too slow, you can:

1. **Disable balance check**: Comment out the balance check section
2. **Skip linting**: Remove clj-kondo (it's non-blocking anyway)
3. **Disable tests**: Comment out the test section (not recommended)

Edit `.git/hooks/pre-commit` and comment out unwanted sections.

## Dependencies

### Required
- `clojure` CLI (required for Polylith check and tests)

### Optional
- `brepl` - File balance checking (https://github.com/yourusername/brepl)
- `clj-kondo` - Linting (https://github.com/clj-kondo/clj-kondo)

Install optional tools:
```bash
# clj-kondo (macOS)
brew install borkdude/brew/clj-kondo

# clj-kondo (Linux)
curl -sLO https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo
chmod +x install-clj-kondo
./install-clj-kondo

# brepl
# (Installation instructions from brepl repository)
```

## Customization

Edit `scripts/pre-commit.sh` to customize checks:

```bash
# Disable balance check
# Comment out the "Checking Clojure/EDN file balance" section

# Make linting blocking
# Change the linting section to exit 1 on warnings

# Skip tests for specific components
# Add conditional to skip certain components

# Add custom checks
# Add new sections before "All checks passed"
```

## CI Integration

The pre-commit hook checks are a **subset** of CI checks. CI also runs:
- Multi-version Java testing (17, 21)
- Security scanning (Trivy)
- Docker build verification
- Full test suite (all components)

**Always ensure commits pass pre-commit hooks** to avoid CI failures.

## See Also

- [CONTRIBUTING.md](../CONTRIBUTING.md) - Contribution guidelines
- [docs/TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md) - Common issues
- [.github/workflows/ci.yml](../.github/workflows/ci.yml) - CI configuration

---

*Install hooks with: `./scripts/install-hooks.sh`*
