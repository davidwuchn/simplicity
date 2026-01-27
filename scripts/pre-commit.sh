#!/usr/bin/env bash
# Pre-commit hook for Simplicity repository
# Install: cp scripts/pre-commit.sh .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit

set -e

echo "🔍 Running pre-commit checks..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check if clojure is installed
if ! command -v clojure &> /dev/null; then
    print_error "clojure CLI not found. Please install it first."
    exit 1
fi

# Check if brepl is installed (for balance checking)
HAS_BREPL=false
if command -v brepl &> /dev/null; then
    HAS_BREPL=true
fi

# Get list of staged Clojure/EDN files
STAGED_CLJ_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(clj|cljs|cljc|edn)$' || true)

if [ -n "$STAGED_CLJ_FILES" ]; then
    echo ""
    echo "📝 Checking Clojure/EDN file balance..."
    
    if [ "$HAS_BREPL" = true ]; then
        BALANCE_FAILED=false
        for file in $STAGED_CLJ_FILES; do
            if [ -f "$file" ]; then
                if ! brepl balance "$file" --dry-run &> /dev/null; then
                    print_error "Balance check failed: $file"
                    brepl balance "$file" --dry-run || true
                    BALANCE_FAILED=true
                fi
            fi
        done
        
        if [ "$BALANCE_FAILED" = true ]; then
            echo ""
            print_error "Some files have unbalanced brackets/parentheses."
            echo "  Run: brepl balance <file> to fix automatically"
            echo "  Or: brepl balance <file> --dry-run to preview fixes"
            exit 1
        else
            print_success "All files are balanced"
        fi
    else
        print_warning "brepl not found - skipping balance check"
        echo "  Install brepl: https://github.com/yourusername/brepl"
    fi
fi

# Run clj-kondo linting
echo ""
echo "🔎 Running clj-kondo linter..."
if command -v clj-kondo &> /dev/null; then
    if clj-kondo --lint src components bases 2>&1 | grep -E "error|warning" > /dev/null; then
        print_warning "clj-kondo found issues:"
        clj-kondo --lint src components bases 2>&1 | grep -E "error|warning" || true
        echo ""
        print_warning "Linting warnings found (not blocking commit)"
    else
        print_success "No linting issues found"
    fi
else
    print_warning "clj-kondo not found - skipping lint check"
    echo "  Install: brew install borkdude/brew/clj-kondo"
    echo "  Or: https://github.com/clj-kondo/clj-kondo/blob/master/doc/install.md"
fi

# Check Polylith workspace
echo ""
echo "🏗️  Checking Polylith workspace..."
if clojure -M:poly check &> /dev/null; then
    print_success "Polylith workspace check passed"
else
    print_error "Polylith workspace check failed"
    clojure -M:poly check
    echo ""
    print_error "Fix workspace issues before committing"
    echo "  Common issues:"
    echo "  - Circular dependencies between components"
    echo "  - Invalid imports (impl namespace used from outside component)"
    echo "  - Missing brick in deps.edn"
    exit 1
fi

# Run tests on changed components
if [ -n "$STAGED_CLJ_FILES" ]; then
    echo ""
    echo "🧪 Running tests for changed code..."
    
    # Extract component names from changed files
    CHANGED_COMPONENTS=$(echo "$STAGED_CLJ_FILES" | grep -oE 'components/[^/]+' | cut -d'/' -f2 | sort -u || true)
    
    if [ -n "$CHANGED_COMPONENTS" ]; then
        TEST_FAILED=false
        for component in $CHANGED_COMPONENTS; do
            echo "  Testing component: $component"
            if ! clojure -M:poly test brick:$component 2>&1 | tail -5; then
                TEST_FAILED=true
                print_error "Tests failed for component: $component"
            fi
        done
        
        if [ "$TEST_FAILED" = true ]; then
            echo ""
            print_error "Some tests failed. Fix them before committing."
            exit 1
        else
            print_success "All component tests passed"
        fi
    else
        # Run all tests if no specific components changed
        echo "  Running all tests..."
        if ! clojure -M:poly test :dev &> /tmp/test-output.txt; then
            print_error "Tests failed"
            tail -20 /tmp/test-output.txt
            exit 1
        else
            print_success "All tests passed"
        fi
    fi
fi

# Check for common issues
echo ""
echo "🔒 Checking for common issues..."

# Check for debug statements
if git diff --cached | grep -E '^\+.*(println|prn|pprint).*"DEBUG' > /dev/null; then
    print_warning "Found debug print statements in staged changes"
    git diff --cached | grep -E '^\+.*(println|prn|pprint).*"DEBUG' || true
    echo ""
    echo "  Consider removing debug statements before committing"
fi

# Check for hardcoded secrets
if git diff --cached | grep -iE '^\+.*(password|secret|api.?key|token)\s*=\s*["\x27]' > /dev/null; then
    print_error "Possible hardcoded secret detected!"
    git diff --cached | grep -iE '^\+.*(password|secret|api.?key|token)\s*=\s*["\x27]' || true
    echo ""
    print_error "Never commit secrets. Use environment variables instead."
    exit 1
fi

# Check for large files
LARGE_FILES=$(git diff --cached --name-only --diff-filter=ACM | xargs -I {} sh -c 'if [ -f "{}" ]; then stat -f%z "{}" 2>/dev/null || stat -c%s "{}"; fi' | awk '$1 > 1048576 {print}' || true)
if [ -n "$LARGE_FILES" ]; then
    print_warning "Large files detected (>1MB):"
    git diff --cached --name-only --diff-filter=ACM | while read file; do
        if [ -f "$file" ]; then
            size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file")
            if [ "$size" -gt 1048576 ]; then
                echo "  $file ($(numfmt --to=iec $size 2>/dev/null || echo "$size bytes"))"
            fi
        fi
    done
    echo ""
    echo "  Consider using Git LFS for large binary files"
fi

# Run sarcasmotron Eight Keys violation check
echo ""
echo "🔥 Running sarcasmotron analysis..."
if [ -f "scripts/sarcasmotron-check.sh" ]; then
    if bash scripts/sarcasmotron-check.sh; then
        print_success "Sarcasmotron check passed"
    else
        SARCA_RESULT=$?
        if [ $SARCA_RESULT -eq 1 ]; then
            print_error "Sarcasmotron found Eight Keys violations"
            echo ""
            echo "  Fix violations before committing. Review:"
            echo "  - [SIMPLICITY.md](./SIMPLICITY.md) for Eight Keys definitions"
            echo "  - [PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md) for implementation guidance"
            exit 1
        elif [ $SARCA_RESULT -eq 0 ]; then
            print_success "Sarcasmotron check passed"
        else
            print_warning "Sarcasmotron check completed with warnings"
        fi
    fi
else
    print_warning "sarcasmotron-check.sh not found - skipping philosophical validation"
    echo "  Consider creating script to enforce Eight Keys principles"
fi

print_success "All checks passed!"

echo ""
echo "✅ Pre-commit checks complete. Proceeding with commit..."
echo ""

exit 0
