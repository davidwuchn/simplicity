#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
# E2E Authentication Test
#
# Tests the complete authentication workflow:
# 1. Signup → Create account → Verify protected page access
# 2. Logout → Verify session cleared
# 3. Login → Verify authentication → Verify protected page access
# 4. Logout → Verify session cleared again
# 5. Protected page → Verify redirect to login (unauthenticated)
#
# Prerequisites:
# - Server running: bb dev
# - agent-browser installed
#
# Usage:
#   ./tests/e2e/test-auth.sh
#   Or via bb: bb test:e2e
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

# Source helpers
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/helpers.sh"

# ═══════════════════════════════════════════════════════════════
# Test Configuration
# ═══════════════════════════════════════════════════════════════

BASE_URL="${BASE_URL:-http://localhost:3000}"
TIMESTAMP=$(date +%s)
USERNAME="e2e_test_${TIMESTAMP}"
PASSWORD="SecurePass123!"
DISPLAY_NAME="E2E Test User"

log_step "E2E Authentication Test Started"
log_info "Test user: $USERNAME"
log_info "Base URL: $BASE_URL"

# ═══════════════════════════════════════════════════════════════
# Prerequisites Check
# ═══════════════════════════════════════════════════════════════

check_agent_browser_installed
check_server_running

# ═══════════════════════════════════════════════════════════════
# Test Step 1: Signup
# ═══════════════════════════════════════════════════════════════

log_step "Test Step 1: Signup"

# Navigate to signup page
log_info "Navigating to signup page"
agent-browser open "${BASE_URL}/signup"

# Get interactive elements
log_info "Getting interactive elements"
ELEMENTS=$(agent-browser snapshot -i)
echo "$ELEMENTS"

# Fill signup form
log_info "Filling signup form"
agent-browser fill @name "$DISPLAY_NAME"
agent-browser fill @username "$USERNAME"
agent-browser fill @password "$PASSWORD"

# Submit form
log_info "Submitting signup form"
agent-browser snapshot -i | grep -q "ESTABLISH LINK" || {
    log_error "Signup button not found"
    exit 1
}
agent-browser click @button

# Wait for redirect to select-game page
log_info "Waiting for redirect to select-game page"
wait_for_url "select-game"

# Verify we're on the select-game page
verify_url_contains "select-game"

# Verify page title
verify_page_title "Select Game"

capture_screenshot "01-signup-success"

log_success "Signup test passed"

# ═══════════════════════════════════════════════════════════════
# Test Step 2: Logout (after signup)
# ═══════════════════════════════════════════════════════════════

log_step "Test Step 2: Logout (after signup)"

log_info "Navigating to logout"
agent-browser open "${BASE_URL}/logout"

# Wait for redirect to login page
log_info "Waiting for redirect to login page"
wait_for_url "login"

# Verify we're on the login page
verify_url_contains "login"
verify_page_title "Login"

capture_screenshot "02-logout-after-signup"

log_success "Logout test passed (after signup)"

# ═══════════════════════════════════════════════════════════════
# Test Step 3: Login
# ═══════════════════════════════════════════════════════════════

log_step "Test Step 3: Login"

# Navigate to login page
log_info "Navigating to login page"
agent-browser open "${BASE_URL}/login"

# Get interactive elements
log_info "Getting interactive elements"
ELEMENTS=$(agent-browser snapshot -i)
echo "$ELEMENTS"

# Fill login form
log_info "Filling login form"
agent-browser fill @username "$USERNAME"
agent-browser fill @password "$PASSWORD"

# Submit form
log_info "Submitting login form"
agent-browser snapshot -i | grep -q "JACK IN" || {
    log_error "Login button not found"
    exit 1
}
agent-browser click @button

# Wait for redirect to select-game page
log_info "Waiting for redirect to select-game page"
wait_for_url "select-game"

# Verify we're on the select-game page
verify_url_contains "select-game"

# Verify page title
verify_page_title "Select Game"

capture_screenshot "03-login-success"

log_success "Login test passed"

# ═══════════════════════════════════════════════════════════════
# Test Step 4: Logout (after login)
# ═══════════════════════════════════════════════════════════════

log_step "Test Step 4: Logout (after login)"

log_info "Navigating to logout"
agent-browser open "${BASE_URL}/logout"

# Wait for redirect to login page
log_info "Waiting for redirect to login page"
wait_for_url "login"

# Verify we're on the login page
verify_url_contains "login"

capture_screenshot "04-logout-after-login"

log_success "Logout test passed (after login)"

# ═══════════════════════════════════════════════════════════════
# Test Step 5: Protected page requires authentication
# ═══════════════════════════════════════════════════════════════

log_step "Test Step 5: Protected page requires authentication"

# Try to access protected page without authentication
log_info "Attempting to access protected page without authentication"
agent-browser open "${BASE_URL}/select-game"

# Wait for redirect to login page
log_info "Waiting for redirect to login page (should happen automatically)"
wait_for_url "login"

# Verify we're on the login page
verify_url_contains "login"
verify_page_title "Login"

capture_screenshot "05-protected-page-redirect"

log_success "Protected page authentication test passed"

# ═══════════════════════════════════════════════════════════════
# Cleanup
# ═══════════════════════════════════════════════════════════════

log_step "Cleanup"

# Close browser
close_browser

# Remove test user from database
cleanup_test_user "$USERNAME"

# ═══════════════════════════════════════════════════════════════
# Test Complete
# ═══════════════════════════════════════════════════════════════

log_step "E2E Authentication Test Complete"
log_success "All tests passed! ✅"
log_info "Screenshots saved to: $SCREENSHOT_DIR"
log_info "Test user cleaned up: $USERNAME"

exit 0
