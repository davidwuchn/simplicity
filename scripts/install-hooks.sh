#!/usr/bin/env bash
# Install Git hooks for Simplicity repository

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_DIR="$(git rev-parse --git-dir)/hooks"

echo "Installing Git hooks..."

# Install pre-commit hook
if [ -f "$SCRIPT_DIR/pre-commit.sh" ]; then
    cp "$SCRIPT_DIR/pre-commit.sh" "$HOOKS_DIR/pre-commit"
    chmod +x "$HOOKS_DIR/pre-commit"
    echo "✓ Installed pre-commit hook"
else
    echo "✗ pre-commit.sh not found in $SCRIPT_DIR"
    exit 1
fi

echo ""
echo "Git hooks installed successfully!"
echo ""
echo "The following checks will run on every commit:"
echo "  - Clojure/EDN file balance (brepl)"
echo "  - Linting (clj-kondo)"
echo "  - Polylith workspace check"
echo "  - Component tests"
echo "  - Common issues (secrets, large files)"
echo ""
echo "To skip hooks (not recommended):"
echo "  git commit --no-verify"
echo ""
