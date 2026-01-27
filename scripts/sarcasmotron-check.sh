#!/usr/bin/env bash
# Sarcasmotron Eight Keys violation checker
# Integrates with pre-commit hooks to enforce philosophical rigor

set -e

echo "🔥 Running sarcasmotron analysis..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_violation() {
    echo -e "${RED}❌ VIOLATION: $1${NC}"
    echo -e "${CYAN}   $2${NC}"
    echo -e "${MAGENTA}   Correct: $3${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Get staged files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM 2>/dev/null || true)

if [[ -z "$STAGED_FILES" ]]; then
    print_info "No staged files to check"
    exit 0
fi

# Track violations
VIOLATION_COUNT=0
WARNING_COUNT=0

# 1. Check for fractal (Clarity) violations - vague language
echo ""
print_info "1. Checking for fractal (Clarity) violations..."

VAGUE_PATTERNS=(
    "handle properly"
    "handle.*edge.*cases"
    "appropriate.*error"
    "valid.*input"
    "safely"
    "properly"
    "correctly"
    "efficiently"
)

for pattern in "${VAGUE_PATTERNS[@]}"; do
    matches=$(echo "$STAGED_FILES" | xargs grep -l -i "$pattern" 2>/dev/null || true)
    
    if [[ -n "$matches" ]]; then
        for file in $matches; do
            # Get context for the violation
            context=$(grep -i -B2 -A2 "$pattern" "$file" | head -5)
            
            print_violation "fractal (Clarity)" \
                "Vague language: '$pattern' in $file" \
                "Define exact validation rules, error codes, or state transitions"
            echo "   Context:"
            echo "$context" | sed 's/^/     /'
            echo ""
            
            ((VIOLATION_COUNT++))
        done
    fi
done

# 2. Check for e (Purpose) violations - abstract nouns
echo ""
print_info "2. Checking for e (Purpose) violations..."

ABSTRACT_NOUN_PATTERNS=(
    "^[^/]*def.*manager"
    "^[^/]*def.*handler"
    "^[^/]*def.*processor"
    "^[^/]*def.*orchestrator"
    "^[^/]*def.*controller"
    "^[^/]*def.*service"
)

for pattern in "${ABSTRACT_NOUN_PATTERNS[@]}"; do
    matches=$(echo "$STAGED_FILES" | xargs grep -l "$pattern" 2>/dev/null || true)
    
    if [[ -n "$matches" ]]; then
        for file in $matches; do
            # Get the violating line
            violating_line=$(grep "$pattern" "$file" | head -1)
            
            print_violation "e (Purpose)" \
                "Abstract noun: '$violating_line' in $file" \
                "Functions must have specific, actionable purpose. Name after what they DO, not what they ARE."
            echo ""
            
            ((VIOLATION_COUNT++))
        done
    fi
done

# 3. Check for ∃ (Truth) violations - ignoring underlying data
echo ""
print_info "3. Checking for ∃ (Truth) violations..."

TRUTH_PATTERNS=(
    "assume.*true"
    "should.*work"
    "probably"
    "likely"
    "maybe"
    "presumably"
)

for pattern in "${TRUTH_PATTERNS[@]}"; do
    matches=$(echo "$STAGED_FILES" | xargs grep -l -i "$pattern" 2>/dev/null || true)
    
    if [[ -n "$matches" ]]; then
        for file in $matches; do
            context=$(grep -i -B1 -A1 "$pattern" "$file" | head -3)
            
            print_violation "∃ (Truth)" \
                "Surface agreement ≠ truth: '$pattern' in $file" \
                "Analyze actual data flows, invariants, and system behavior. Verify assumptions."
            echo "   Context: $context"
            echo ""
            
            ((VIOLATION_COUNT++))
        done
    fi
done

# 4. Check for ∀ (Vigilance) violations - accepting manipulation
echo ""
print_info "4. Checking for ∀ (Vigilance) violations..."

VIGILANCE_PATTERNS=(
    "trust.*input"
    "assume.*valid"
    "skip.*validation"
    "TODO"
    "FIXME"
    "XXX"
    "HACK"
)

for pattern in "${VIGILANCE_PATTERNS[@]}"; do
    matches=$(echo "$STAGED_FILES" | xargs grep -l -i "$pattern" 2>/dev/null || true)
    
    if [[ -n "$matches" ]]; then
        for file in $matches; do
            context=$(grep -i -B1 -A1 "$pattern" "$file" | head -3)
            
            if [[ "$pattern" == "TODO" || "$pattern" == "FIXME" || "$pattern" == "XXX" || "$pattern" == "HACK" ]]; then
                print_violation "∀ (Vigilance)" \
                    "Placeholder technical debt: '$pattern' in $file" \
                    "Define concrete implementation steps or remove the placeholder. Technical debt has zero interest."
            else
                print_violation "∀ (Vigilance)" \
                    "Accepting manipulation: '$pattern' in $file" \
                    "Verify claims, question assumptions, demand evidence. You're the brakes, not engine."
            fi
            echo "   Context: $context"
            echo ""
            
            ((VIOLATION_COUNT++))
        done
    fi
done

# 5. Check for φ (Vitality) violations - mechanical repetition
echo ""
print_info "5. Checking for φ (Vitality) violations..."

# Look for repetitive patterns (more than 3 similar lines in a row)
for file in $STAGED_FILES; do
    if [[ -f "$file" && "$file" =~ \.(clj|cljs|cljc|java|js)$ ]]; then
        # Simple check for repeated lines (basic vitality check)
        repeats=$(awk 'count[$0]++ {if (count[$0]==3) print}' "$file" | wc -l)
        
        if [[ "$repeats" -gt 0 ]]; then
            print_warning "Potential φ (Vitality) violation: Mechanical repetition in $file"
            echo "   Consider refactoring repeated patterns into functions"
            ((WARNING_COUNT++))
        fi
    fi
done

# 6. Check for π (Synthesis) violations - incomplete mental models
echo ""
print_info "6. Checking for π (Synthesis) violations..."

# Look for functions without proper error handling or edge cases
for file in $STAGED_FILES; do
    if [[ -f "$file" && "$file" =~ \.(clj|cljs|cljc)$ ]]; then
        # Check for defn without try/catch for I/O operations
        io_functions=$(grep -n "defn.*\(http\|db\|file\|io\|read\|write\)" "$file" 2>/dev/null || true)
        
        if [[ -n "$io_functions" ]]; then
            while IFS= read -r line; do
                function_name=$(echo "$line" | sed -E 's/.*defn[[:space:]]+([^[[:space:]]+).*/\1/')
                line_num=$(echo "$line" | cut -d: -f1)
                
                # Check if function has error handling
                if ! sed -n "$((line_num)),$((line_num+20))p" "$file" | grep -q "try\|catch\|ex-info"; then
                    print_warning "Potential π (Synthesis) violation: Incomplete error handling in $file:$line_num"
                    echo "   Function '$function_name' does I/O without error handling"
                    echo "   Consider adding try/catch or proper error propagation"
                    ((WARNING_COUNT++))
                fi
            done <<< "$io_functions"
        fi
    fi
done

# 7. Check for mathematical principle application
echo ""
print_info "7. Checking for mathematical principle application..."

# Verify that at least some files reference mathematical concepts
MATH_TERMS=("orthogonal" "invariant" "complexity" "logarithm" "calculus" "chaos" "information")
math_references=0

for term in "${MATH_TERMS[@]}"; do
    term_refs=$(echo "$STAGED_FILES" | xargs grep -l -i "$term" 2>/dev/null | wc -l || echo 0)
    math_references=$((math_references + term_refs))
done

if [[ "$math_references" -eq 0 && "$(echo "$STAGED_FILES" | grep -c '\.md$' || echo 0)" -gt 0 ]]; then
    print_warning "No mathematical principle references found in documentation"
    echo "   Consider connecting implementation decisions to mathematical foundations"
    ((WARNING_COUNT++))
fi

# Summary
echo ""
echo "="*50
echo "🔥 Sarcasmotron Analysis Complete"
echo "="*50

if [[ "$VIOLATION_COUNT" -eq 0 && "$WARNING_COUNT" -eq 0 ]]; then
    print_success "No Eight Keys violations detected!"
    echo ""
    print_info "Commit adheres to mathematical rigor and philosophical principles."
    exit 0
elif [[ "$VIOLATION_COUNT" -eq 0 ]]; then
    print_warning "$WARNING_COUNT warnings found (no critical violations)"
    echo ""
    print_info "Commit mostly adheres to principles. Review warnings for improvement."
    exit 0
else
    print_violation "TOTAL" \
        "$VIOLATION_COUNT critical violations and $WARNING_COUNT warnings found" \
        "Fix violations before committing. See above for specific corrections."
    echo ""
    print_info "Review [SIMPLICITY.md](../SIMPLICITY.md) for Eight Keys definitions."
    print_info "Review [PRACTICAL_GUIDE.md](../PRACTICAL_GUIDE.md) for implementation guidance."
    exit 1
fi