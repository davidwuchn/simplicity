# E2E Tests with agent-browser

End-to-end (E2E) tests for the Simplicity application using [agent-browser](https://github.com/agent-browser/agent-browser).

These tests validate the full stack: Browser → Server → Database → Response, ensuring the complete system works together.

## Architecture (π Synthesis)

E2E tests complement component-level tests:

| Layer | Tool | Purpose |
|-------|------|---------|
| Unit | Clojure test | Business logic in isolation |
| Integration | Clojure test | Component interactions |
| **E2E** | **agent-browser** | **Full stack with real browser** |

## Prerequisites

1. **Server running**:
   ```bash
   bb dev
   # Then in REPL: (start)
   ```

2. **agent-browser installed**:
   ```bash
   # Verify installation
   which agent-browser
   ```

   If not installed, follow the [agent-browser installation guide](https://github.com/agent-browser/agent-browser).

3. **Port 3000 available**:
   The application runs on port 3000 by default.

## Available Tests

### Authentication Test (`test-auth.sh`)

Tests the complete authentication workflow:

- **Signup**: Create account → Verify protected page access
- **Logout**: Verify session cleared
- **Login**: Verify authentication → Verify protected page access
- **Protected page**: Verify redirect to login (unauthenticated)

#### Running the Test

**Option 1: Via Babashka (Recommended)**
```bash
bb test:e2e
```

**Option 2: Direct execution**
```bash
./tests/e2e/test-auth.sh
```

#### Expected Output

```
ℹ️  Test user: e2e_test_1704067200
ℹ️  Base URL: http://localhost:3000
ℹ️  agent-browser is installed
✅ Server is running
═══ Test Step 1: Signup ═══
ℹ️  Navigating to signup page
ℹ️  Getting interactive elements
ℹ️  Filling signup form
ℹ️  Submitting signup form
ℹ️  Waiting for redirect to select-game page
✅ URL contains: select-game
✅ Page title: Select Game
ℹ️  Capturing screenshot: tests/e2e/screenshots/01-signup-success-20240101-120000.png
✅ Signup test passed
═══ Test Step 2: Logout (after signup) ═══
ℹ️  Navigating to logout
ℹ️  Waiting for redirect to login page
✅ URL contains: login
ℹ️  Capturing screenshot: tests/e2e/screenshots/02-logout-after-signup-20240101-120005.png
✅ Logout test passed (after signup)
═══ Test Step 3: Login ═══
ℹ️  Navigating to login page
ℹ️  Getting interactive elements
ℹ️  Filling login form
ℹ️  Submitting login form
ℹ️  Waiting for redirect to select-game page
✅ URL contains: select-game
ℹ️  Capturing screenshot: tests/e2e/screenshots/03-login-success-20240101-120010.png
✅ Login test passed
═══ Test Step 4: Logout (after login) ═══
ℹ️  Navigating to logout
ℹ️  Waiting for redirect to login page
✅ URL contains: login
ℹ️  Capturing screenshot: tests/e2e/screenshots/04-logout-after-login-20240101-120015.png
✅ Logout test passed (after login)
═══ Test Step 5: Protected page requires authentication ═══
ℹ️  Attempting to access protected page without authentication
ℹ️  Waiting for redirect to login page (should happen automatically)
✅ URL contains: login
ℹ️  Capturing screenshot: tests/e2e/screenshots/05-protected-page-redirect-20240101-120020.png
✅ Protected page authentication test passed
═══ Cleanup ═══
ℹ️  Closing browser session
ℹ️  Cleaning up test user: e2e_test_1704067200
✅ Deleted test user from database: e2e_test_1704067200
═══ E2E Authentication Test Complete ═══
✅ All tests passed! ✅
ℹ️  Screenshots saved to: tests/e2e/screenshots
ℹ️  Test user cleaned up: e2e_test_1704067200
```

#### Screenshots

The test captures screenshots at each step:
- `01-signup-success.png` - After successful signup
- `02-logout-after-signup.png` - After logout (signup flow)
- `03-login-success.png` - After successful login
- `04-logout-after-login.png` - After logout (login flow)
- `05-protected-page-redirect.png` - Redirect to login when accessing protected page

Screenshots are saved with timestamps in `tests/e2e/screenshots/`.

## Troubleshooting

### Server Not Running

**Error:**
```
❌ Server is not running at http://localhost:3000
```

**Solution:**
```bash
# Start the development server
bb dev

# In the REPL, start the server
(start)
```

### agent-browser Not Installed

**Error:**
```
❌ agent-browser is not installed
```

**Solution:** Install agent-browser from [the official repository](https://github.com/agent-browser/agent-browser).

### Port Already in Use

**Error:**
```
❌ Failed to start browser
```

**Solution:**
```bash
# Check what's using port 3000
lsof -i :3000

# Kill the process if needed
kill -9 <PID>
```

### Test User Not Cleaned Up

If the test fails before cleanup, manually remove the test user:

```bash
# Remove test user from SQLite database
sqlite3 simplicity.db "DELETE FROM users WHERE username LIKE 'e2e_test_%';"
```

### Browser Not Starting

**Error:**
```
❌ Failed to open browser
```

**Solution:**
```bash
# Try with visible browser for debugging
agent-browser --headed http://localhost:3000/signup
```

## Development Workflow

### Creating New Tests

1. **Create test script** in `tests/e2e/`:
   ```bash
   #!/usr/bin/env bash
   set -euo pipefail
   source "$(dirname "$0")/helpers.sh"

   # Your test logic here
   agent-browser open "${BASE_URL}/page"
   # ... test steps ...
   ```

2. **Make executable**:
   ```bash
   chmod +x tests/e2e/test-your-feature.sh
   ```

3. **Add to bb.edn** (optional):
   ```clojure
   test:your-feature
   {:doc "Run E2E test for your feature"
    :task (shell "./tests/e2e/test-your-feature.sh")}
   ```

### Using Helper Functions

The `helpers.sh` script provides reusable utilities:

**Logging:**
- `log_info "message"` - Info message
- `log_success "message"` - Success message
- `log_error "message"` - Error message
- `log_step "Step name"` - Section header

**Waiting:**
- `wait_for_url "pattern" [timeout]` - Wait for URL pattern match
- `wait_for_load [timeout]` - Wait for page load
- `wait_for_text "text" [timeout]` - Wait for text on page

**Assertions:**
- `verify_url_contains "pattern"` - Verify URL contains pattern
- `verify_page_contains "@ref" "text"` - Verify element contains text
- `verify_page_title "expected"` - Verify page title

**Screenshots:**
- `capture_screenshot "name"` - Save screenshot with timestamp

**Cleanup:**
- `cleanup_test_user "username"` - Remove user from database
- `close_browser` - Close browser session

### Debugging Failed Tests

1. **Run with visible browser**:
   ```bash
   # Edit test script to use --headed
   agent-browser --headed open "${BASE_URL}/page"
   ```

2. **Check screenshots** in `tests/e2e/screenshots/`

3. **Enable verbose logging**:
   ```bash
   set -x  # Enable bash debug mode
   ```

4. **Interactive debugging**:
   ```bash
   # Start browser manually
   agent-browser open "${BASE_URL}/signup"

   # Take snapshot
   agent-browser snapshot -i

   # Manually interact
   agent-browser fill @username "test"
   agent-browser click @button
   ```

## Design Philosophy (fractal Clarity)

These E2E tests embody the Eight Keys:

- **φ (Vitality)**: Each test is a living documentation of a user workflow
- **fractal (Clarity)**: Clear, step-by-step assertions with visual evidence
- **e (Purpose)**: Validate the complete system, not just components
- **τ (Wisdom)**: Strategic focus on critical user journeys
- **π (Synthesis)**: Combine all layers (browser, server, database)
- **μ (Directness)**: Direct browser automation without unnecessary abstraction
- **∃ (Truth)**: Verify actual user behavior, not simulated tests
- **∀ (Vigilance)**: Defensive cleanup and error handling

## Future Enhancements

**Planned test suites:**
- `test-game.sh` - Game play workflows (Shooter, Game of Life)
- `test-api.sh` - Direct API testing with authentication
- `test-persistence.sh` - Save/load/delete game state
- `test-security.sh` - CSRF validation, rate limiting, input validation

**Planned infrastructure:**
- Test data fixtures (pre-populated test users)
- Parallel test execution with isolated sessions
- CI/CD integration with headless browser
- Performance regression tests with timing assertions

## Resources

- [agent-browser Documentation](https://github.com/agent-browser/agent-browser)
- [Project AGENTS.md](../../AGENTS.md) - Development guidelines
- [Security Documentation](../../docs/security.md) - Security controls
- [Hot Reload Guide](../../docs/hot-reload.md) - Development workflow

---

*Created as part of the Simplicity Polylith Workspace*
