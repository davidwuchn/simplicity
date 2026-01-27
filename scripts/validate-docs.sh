#!/usr/bin/env bash
# Documentation validation script for Simplicity repository
# Validates cross-references, broken links, and documentation consistency

set -e

echo "📚 Validating documentation consistency..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check for required commands
REQUIRED_CMDS=("grep" "find" "sed")
for cmd in "${REQUIRED_CMDS[@]}"; do
    if ! command -v "$cmd" &> /dev/null; then
        print_error "Command '$cmd' not found"
        exit 1
    fi
done

# Track validation results
ERRORS=0
WARNINGS=0

# 1. Check for broken markdown links
echo ""
print_info "1. Checking for broken markdown links..."

find . -name "*.md" -type f ! -path "./.*" ! -path "./target/*" ! -path "./node_modules/*" | while read -r file; do
    # Extract all markdown links from file
    grep -o '\[[^]]*\]([^)]*)' "$file" | while read -r link; do
        # Extract the file path from the link
        linked_file=$(echo "$link" | sed -E 's/.*\((.*)\)/\1/' | sed 's/#.*//')
        
        # Skip external links
        if [[ "$linked_file" =~ ^(http|https|ftp|mailto): ]]; then
            continue
        fi
        
        # Skip empty or anchor-only links
        if [[ -z "$linked_file" || "$linked_file" == "#"* ]]; then
            continue
        fi
        
        # Resolve relative path
        if [[ "$linked_file" != /* ]]; then
            linked_file="$(dirname "$file")/$linked_file"
        fi
        
        # Check if file exists
        if [[ ! -f "$linked_file" && ! -d "$linked_file" ]]; then
            print_error "Broken link in $file: $link → $linked_file"
            ((ERRORS++))
        fi
    done
done

# 2. Verify three-document hierarchy references
echo ""
print_info "2. Verifying three-document hierarchy references..."

REQUIRED_DOCS=("SIMPLICITY.md" "PRACTICAL_GUIDE.md" "AGENTS.md")
for doc in "${REQUIRED_DOCS[@]}"; do
    references=$(grep -r "$doc" --include="*.md" . 2>/dev/null | wc -l || echo 0)
    
    if [[ "$references" -eq 0 ]]; then
        print_warning "Document '$doc' not referenced in other documentation"
        ((WARNINGS++))
    else
        print_success "Document '$doc' has $references references"
    fi
done

# 3. Check for sarcasmotron violations in documentation
echo ""
print_info "3. Checking for sarcasmotron violations..."

# Common violation patterns
VIOLATION_PATTERNS=(
    "handle properly"
    "should be"
    "might be"
    "could be"
    "TODO"
    "FIXME"
    "XXX"
    "HACK"
)

for pattern in "${VIOLATION_PATTERNS[@]}"; do
    violations=$(grep -r -i "$pattern" --include="*.md" . 2>/dev/null | grep -v "scripts/validate-docs.sh" | wc -l || echo 0)
    
    if [[ "$violations" -gt 0 ]]; then
        print_warning "Found $violations instances of '$pattern' (sarcasmotron violation)"
        ((WARNINGS++))
        
        # Show first few instances
        if [[ "$violations" -le 5 ]]; then
            grep -r -i "$pattern" --include="*.md" . 2>/dev/null | grep -v "scripts/validate-docs.sh" | head -3 | while read -r line; do
                echo "   $line"
            done
        fi
    fi
done

# 4. Verify test assertion count matches documentation
echo ""
print_info "4. Verifying test assertion count..."

# Try to get actual test count
if command -v clojure &> /dev/null; then
    print_info "Running tests to count assertions..."
    
    # Create a temporary test runner to count assertions
    TEMP_FILE=$(mktemp)
    cat > "$TEMP_FILE" << 'EOF'
(ns test-counter
  (:require [clojure.test :as t]
            [clojure.string :as str]))

(def assertion-count (atom 0))

(defmethod t/report :pass [m]
  (swap! assertion-count inc))

(defmethod t/report :fail [m]
  (swap! assertion-count inc))

(defmethod t/report :error [m]
  (swap! assertion-count inc))

(defn -main []
  (t/run-all-tests)
  (println @assertion-count))

(-main)
EOF
    
    # Run test counting
    TEST_COUNT=$(clojure -Sdeps '{:deps {org.clojure/clojure {:mvn/version "1.11.1"}}}' -M -m test-counter 2>/dev/null || echo "0")
    rm "$TEMP_FILE"
    
    # Check README for assertion count
    README_COUNT=$(grep -oE "[0-9]+ passing assertions" README.md | grep -oE "[0-9]+" || echo "0")
    PRACTICAL_GUIDE_COUNT=$(grep -oE "[0-9]+ passing assertions" PRACTICAL_GUIDE.md | grep -oE "[0-9]+" || echo "0")
    
    if [[ "$TEST_COUNT" != "0" && "$README_COUNT" != "0" ]]; then
        if [[ "$TEST_COUNT" != "$README_COUNT" ]]; then
            print_error "Test count mismatch: Tests report $TEST_COUNT, README.md claims $README_COUNT"
            ((ERRORS++))
        else
            print_success "Test assertion count matches: $TEST_COUNT"
        fi
        
        if [[ "$TEST_COUNT" != "$PRACTICAL_GUIDE_COUNT" && "$PRACTICAL_GUIDE_COUNT" != "0" ]]; then
            print_error "Test count mismatch: Tests report $TEST_COUNT, PRACTICAL_GUIDE.md claims $PRACTICAL_GUIDE_COUNT"
            ((ERRORS++))
        fi
    else
        print_warning "Could not verify test assertion count automatically"
        ((WARNINGS++))
    fi
else
    print_warning "Clojure not found - skipping test assertion verification"
    ((WARNINGS++))
fi

# 5. Check for placeholder secrets
echo ""
print_info "5. Checking for placeholder secrets..."

SECRET_PATTERNS=(
    "xxxxx"
    "password"
    "secret.*key"
    "api.*token"
    "dop_v1_"
)

for pattern in "${SECRET_PATTERNS[@]}"; do
    secrets=$(grep -r -i "$pattern" --include="*.md" --include="*.tf" --include="*.sh" . 2>/dev/null | 
              grep -v "scripts/validate-docs.sh" | 
              grep -v "terraform.tfvars.example" |
              wc -l || echo 0)
    
    if [[ "$secrets" -gt 0 ]]; then
        print_warning "Found $secrets potential placeholder secrets with pattern '$pattern'"
        ((WARNINGS++))
    fi
done

# 6. Verify mathematical grounding references
echo ""
print_info "6. Verifying mathematical grounding..."

MATH_FILES=("SIMPLICITY.md" "EQUATIONS_FOR_WORLD.md")
for file in "${MATH_FILES[@]}"; do
    if [[ -f "$file" ]]; then
        # Check for mathematical equations
        equations=$(grep -c '\$' "$file" || echo 0)
        if [[ "$equations" -gt 0 ]]; then
            print_success "$file contains $equations mathematical equations"
        else
            print_warning "$file contains no mathematical equations"
            ((WARNINGS++))
        fi
    else
        print_error "Mathematical file $file not found"
        ((ERRORS++))
    fi
done

# Summary
echo ""
echo "="*50
echo "📊 Documentation Validation Summary"
echo "="*50

if [[ "$ERRORS" -eq 0 && "$WARNINGS" -eq 0 ]]; then
    print_success "All checks passed! Documentation is consistent."
    exit 0
elif [[ "$ERRORS" -eq 0 ]]; then
    print_warning "$WARNINGS warnings found (no errors)"
    echo ""
    echo "⚠️  Recommendations:"
    echo "  - Review warnings above"
    echo "  - Consider fixing sarcasmotron violations"
    echo "  - Update placeholder text with actual instructions"
    exit 0
else
    print_error "$ERRORS errors and $WARNINGS warnings found"
    echo ""
    echo "❌ Critical issues:"
    echo "  - Fix broken links"
    echo "  - Ensure test counts match"
    echo "  - Verify mathematical files exist"
    exit 1
fi