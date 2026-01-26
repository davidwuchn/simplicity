#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
# E2E Test Helpers for agent-browser
#
# Reusable functions for end-to-end testing with agent-browser.
# Provides utilities for waiting, assertions, screenshots, and cleanup.
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

# ═══════════════════════════════════════════════════════════════
# Configuration
# ═══════════════════════════════════════════════════════════════

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCREENSHOT_DIR="${SCRIPT_DIR}/screenshots"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")

# Ensure screenshot directory exists
mkdir -p "$SCREENSHOT_DIR"

# ═══════════════════════════════════════════════════════════════
# Logging Functions
# ═══════════════════════════════════════════════════════════════

log_info() {
    echo "ℹ️  $*"
}

log_success() {
    echo "✅ $*"
}

log_error() {
    echo "❌ $*"
}

log_step() {
    echo "═══ $* ═══"
}

# ═══════════════════════════════════════════════════════════════
# Wait Functions
# ═══════════════════════════════════════════════════════════════

wait_for_url() {
    local pattern="$1"
    local timeout="${2:-10000}"  # Default 10 seconds
    log_info "Waiting for URL pattern: $pattern (timeout: ${timeout}ms)"
    agent-browser wait --url "$pattern" --timeout "$timeout"
}

wait_for_load() {
    local timeout="${1:-5000}"  # Default 5 seconds
    log_info "Waiting for page load (timeout: ${timeout}ms)"
    agent-browser wait --load networkidle --timeout "$timeout"
}

wait_for_text() {
    local text="$1"
    local timeout="${2:-5000}"  # Default 5 seconds
    log_info "Waiting for text: '$text' (timeout: ${timeout}ms)"
    agent-browser wait --text "$text" --timeout "$timeout"
}

# ═══════════════════════════════════════════════════════════════
# Assertion Functions
# ═══════════════════════════════════════════════════════════════

verify_url_contains() {
    local expected="$1"
    local actual
    actual=$(agent-browser get url)
    if [[ "$actual" != *"$expected"* ]]; then
        log_error "URL verification failed"
        log_error "  Expected to contain: $expected"
        log_error "  Actual: $actual"
        return 1
    fi
    log_success "URL contains: $expected"
}

verify_page_contains() {
    local element_ref="$1"
    local expected_text="$2"

    local actual_text
    actual_text=$(agent-browser get text "$element_ref")

    if [[ "$actual_text" != *"$expected_text"* ]]; then
        log_error "Text verification failed for element $element_ref"
        log_error "  Expected to contain: $expected_text"
        log_error "  Actual: $actual_text"
        return 1
    fi
    log_success "Element $element_ref contains: $expected_text"
}

verify_page_title() {
    local expected="$1"
    local actual
    actual=$(agent-browser get title)
    if [[ "$actual" != "$expected" ]]; then
        log_error "Page title verification failed"
        log_error "  Expected: $expected"
        log_error "  Actual: $actual"
        return 1
    fi
    log_success "Page title: $expected"
}

# ═══════════════════════════════════════════════════════════════
# Screenshot Functions
# ═══════════════════════════════════════════════════════════════

capture_screenshot() {
    local name="$1"
    local filename="${SCREENSHOT_DIR}/${name}-${TIMESTAMP}.png"
    log_info "Capturing screenshot: $filename"
    agent-browser screenshot "$filename"
    log_success "Screenshot saved: $filename"
}

# ═══════════════════════════════════════════════════════════════
# Cleanup Functions
# ═══════════════════════════════════════════════════════════════

cleanup_test_user() {
    local username="$1"
    log_info "Cleaning up test user: $username"

    # Check if server is running
    if ! curl -s http://localhost:3000/health > /dev/null; then
        log_error "Server not running at http://localhost:3000"
        return 1
    fi

    # Delete user via direct SQL (SQLite)
    local db_path="${DB_PATH:-./simplicity.db}"
    if [[ -f "$db_path" ]]; then
        sqlite3 "$db_path" "DELETE FROM users WHERE username = '$username';" 2>/dev/null || true
        log_success "Deleted test user from database: $username"
    else
        log_info "Database not found at $db_path, skipping cleanup"
    fi
}

# ═══════════════════════════════════════════════════════════════
# Setup/Teardown Functions
# ═══════════════════════════════════════════════════════════════

check_server_running() {
    log_info "Checking if server is running..."
    if ! curl -s http://localhost:3000/health > /dev/null; then
        log_error "Server is not running at http://localhost:3000"
        log_error "Please start the server with: bb dev"
        return 1
    fi
    log_success "Server is running"
}

check_agent_browser_installed() {
    if ! command -v agent-browser &> /dev/null; then
        log_error "agent-browser is not installed"
        log_error "Install it from: https://github.com/agent-browser/agent-browser"
        return 1
    fi
    log_success "agent-browser is installed"
}

close_browser() {
    log_info "Closing browser session"
    agent-browser close 2>/dev/null || true
}
