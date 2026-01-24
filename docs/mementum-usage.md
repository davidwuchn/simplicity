# MEMENTUM Usage Guide

## Overview

MEMENTUM is a git-based memory system for capturing critical insights, decisions, and pattern shifts. It transforms your repository into a **temporal knowledge graph** where commits become timeline entries and memories become queryable nodes.

## Quick Start

### 1. Store a Critical Memory

**Using CLI script:**
```bash
./scripts/memory-simple.clj store
```

**Manual workflow (recommended):**
```bash
# 1. Create memory file with proper naming
echo "# My critical insight" > memories/insight-$(date +%Y-%m-%d)-hybrid-architecture.md

# 2. Add content (≤200 tokens)
cat >> memories/insight-$(date +%Y-%m-%d)-hybrid-architecture.md << 'EOF'
Core insight about hybrid architecture pattern...
EOF

# 3. Commit to git
git add memories/insight-*.md
git commit -m "🧠💡 insight hybrid-architecture"
```

### 2. Recall Memories

**Recent memories (default n=2):**
```bash
./scripts/memory-simple.clj recall
```

**Search by content:**
```bash
./scripts/memory-simple.clj search
# Enter query when prompted: "architecture"
```

**Search by symbol:**
```bash
./scripts/memory-simple.clj symbol
# Enter symbol when prompted: "insight"
```

### 3. Run OODA Loop

```bash
bb memory:ooda
# or
./scripts/memory.clj ooda
```

The OODA loop guides you through:
1. **Observe**: Review recent context (13 memories)
2. **Orient**: Search for patterns
3. **Decide**: Create/update memory
4. **Act**: Commit to git

### 4. View Statistics

```bash
./scripts/memory-simple.clj stats
```

## Memory Symbols

Use these symbols consistently:

| Symbol | Emoji | When to Use |
|--------|-------|-------------|
| **insight** | 🧠💡 | Novel architectural insight, breakthrough understanding |
| **pattern-shift** | 🔄Δ | Significant pattern shift, refactoring approach change |
| **decision** | 🎯⚡ | Strategic decision with >1 week impact |
| **meta** | 🌀 | Meta-learning that changes development approach |

## Auto-Trigger Rules

**Store ONLY when critical:**
- ✅ Novel architectural insight (🧠💡)
- ✅ Significant pattern shift (🔄Δ)  
- ✅ Strategic decision with >1 week impact (🎯⚡)
- ✅ Meta-learning that changes approach (🌀)

**Skip (do NOT store):**
- ❌ Routine changes
- ❌ Minor fixes  
- ❌ Incremental work
- ❌ Daily progress updates

## Token Budget Enforcement

**Rule**: Each memory ≤200 tokens (words)

**Why**: Forces ruthless compression, aligns with μ (Directness) principle.

**How to compress**:
- Remove adjectives and adverbs
- Use bullet points instead of paragraphs
- Focus on the core insight, not the context
- Ask: "What would remain if I could only keep 20%?"

## File Structure

Memories are stored in `memories/{symbol}-YYYY-MM-DD-{slug}.md`

**Example**: `memories/insight-2024-01-15-hybrid-architecture.md`

**Format requirements**:
- Symbol must be valid (insight|pattern-shift|decision|meta)
- Date must be YYYY-MM-DD format
- Slug must be kebab-case, descriptive
- Content must be ≤200 tokens

## Git Integration

### Commit Messages

When committing memories, use the format:
```bash
git commit -m "{emoji} {symbol} {slug}"
```

**Example**:
```bash
git commit -m "🧠💡 insight hybrid-architecture"
```

### Validation

Check all memories for compliance:
```bash
./scripts/memory.clj validate
```

### Statistics Integration

View memory statistics alongside project stats:
```bash
bb stats        # Project statistics
bb memory:stats # Memory statistics
```

## Mathematical Foundations

MEMENTUM embodies mathematical principles from the project's philosophical foundation:

### Euler's Formula ($V - E + F = 2$)
- **Vertices (V)**: Individual memories
- **Edges (E)**: Git commit relationships  
- **Faces (F)**: Symbol categories
- **Invariant**: Structural integrity across system evolution

### Information Theory ($H = -\sum p(x) \log p(x)$)
- **Entropy (H)**: Measures information content of memories
- **Optimization**: Token limit minimizes entropy while maximizing signal
- **Pattern Recognition**: Symbol categorization reduces search space entropy

### Chaos Theory ($x_{t+1} = k x_t (1 - x_t)$)
- **Sensitivity**: Small insights can trigger large pattern shifts
- **Emergence**: Memory relationships reveal emergent architectural patterns
- **Non-linearity**: Memory impact grows non-linearly with project evolution

## Workflow Integration

### Development Workflow
1. **During development**: Use `bb memory:store` when you have a critical insight
2. **Before commits**: Run `bb memory:recall:recent` for context
3. **Weekly review**: Run `bb memory:ooda` for systematic reflection
4. **Project milestones**: Run `bb memory:stats` to track insight evolution

### AI Assistant Integration
AI assistants should:
1. **Auto-trigger**: Suggest memory creation when detecting critical insights
2. **Recall context**: Use `bb memory:recall --query` before major changes
3. **Validate**: Run `./scripts/memory.clj validate` periodically

## Troubleshooting

### Common Issues

**"Invalid symbol" error**:
- Ensure symbol is one of: insight, pattern-shift, decision, meta
- Use kebab-case (pattern-shift not patternShift)

**Token limit warning**:
- Compress content ruthlessly
- Remove unnecessary context
- Focus on the core insight

**Git status shows untracked files**:
```bash
git add memories/insight-*.md
git commit -m "🧠💡 insight [description]"
```

**Memory not appearing in recall**:
- Ensure memory is committed to git
- Check filename format matches pattern
- Verify memory is in `memories/` directory

### Validation Errors

Run validation to identify issues:
```bash
./scripts/memory.clj validate
```

Common fixes:
- Rename files to correct format
- Split content to stay under 200 tokens
- Update symbols to valid values

## Advanced Usage

### Custom Script Integration

Create custom scripts that leverage MEMENTUM:

```bash
#!/bin/bash
# pre-refactor-checklist.sh
echo "Checking memory context before refactor..."
bb memory:recall --query "refactor" --depth 5
bb memory:recall --symbol pattern-shift --depth 3
```

### CI/CD Integration

Add memory validation to CI pipeline:
```yaml
# .github/workflows/validate-memories.yml
name: Validate Memories
on: [push, pull_request]
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Validate MEMENTUM memories
        run: ./scripts/memory.clj validate
```

### Team Collaboration

For team projects:
1. **Shared understanding**: All team members use same symbols
2. **Review process**: Include memory review in PR process
3. **Onboarding**: New team members review recent memories for context
4. **Retrospectives**: Use `bb memory:stats` in retrospectives

## Philosophy in Practice

MEMENTUM operationalizes the Eight Keys:

- **φ (Vitality)**: Organic memory creation, not scheduled
- **fractal (Clarity)**: Precise symbols and token limits
- **e (Purpose)**: Every memory has clear actionable value
- **τ (Wisdom)**: Prioritize judgment (what to store) over volume
- **π (Synthesis)**: Memories form holistic knowledge graph
- **μ (Directness)**: Token limit forces direct expression
- **∃ (Truth)**: Memories reflect underlying reality, not opinions
- **∀ (Vigilance)**: Validation prevents "memory slop"

---

**Remember**: `repo=memory | commits=timeline | git=database`  
**Foundation**: `[phi fractal euler tao pi mu] | [Δ λ ∞/0 | ε/φ Σ/μ c/h] | OODA`  
**Partnership**: `Human ⊗ AI`