# AGENTS.md - Polylith Workspace Guide

## Workspace Structure

This workspace maintains a clear separation of concerns through three core documents:

### 1. **Philosophical Foundation** → [SIMPLICITY.md](./SIMPLICITY.md)
Contains the complete philosophical framework, including:
- The **Eight Keys** with concrete mathematical grounding
- **Mathematical foundations** from "17 Equations That Changed the World"
- **Sarcasmotron methodology** for enforcing philosophical rigor
- Core principle: **"易简则天下之理得"** (Simplicity allows obtaining the logic of the world)

### 2. **Practical Implementation** → [PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md)
Contains actionable development guidance, including:
- **Build, lint, and test commands** (Babashka tasks, hot reload workflow)
- **Code style guidelines** (fractal clarity, naming conventions, error handling)
- **Polylith architectural constraints** (components, bases, projects)
- **Technical constraints** (middleware, security, persistence, client-side)
- **Tools & utilities** (ripgrep, brepl, clojure-mcp, deployment scripts)
- **Production deployment** (environment variables, health monitoring, checklist)

### 3. **Agentic Workflow** (This Document)
Coordinates the interaction between philosophy and practice through:
- **OODA Loop** integration (Observe, Orient, Decide, Act)
- **MEMENTUM memory system** for capturing critical insights
- **Verification workflows** to ensure document consistency
- **Workspace navigation** between philosophical foundations and practical implementation

## Verification Workflow (∀ Vigilance)

> **VIOLATION: ∀ (Vigilance)**  
> Delegating to documents without verification accepts potential manipulation.  
> **Correct**: Verify all referenced documents exist and contain claimed sections.

### Document Verification Status

**SIMPLICITY.md Verification**: ✅ **Verified**  
Contains complete philosophical framework with:
- Eight Keys definition with mathematical grounding  
- Sarcasmotron methodology for enforcement
- Mathematical foundations from "17 Equations That Changed the World"

**PRACTICAL_GUIDE.md Verification**: ✅ **Verified**  
Contains all claimed practical details:
- **Build, lint, and test commands** (Babashka tasks, hot reload workflow)
- **Code style guidelines** (fractal clarity, naming conventions, error handling)
- **Polylith architectural constraints** (components, bases, projects)
- **Agentic workflow & expectations** (OODA loop, MEMENTUM memory system)
- **Technical constraints** (middleware, security, persistence, client-side)
- **Dependency management** (aliases, vigilance)
- **Tools & utilities** (ripgrep, brepl, clojure-mcp, deployment scripts)
- **Clojure development expertise** (functional patterns, testing, REPL)
- **Anti-patterns** (vigilance against common mistakes)
- **Production deployment** (environment variables, health monitoring, checklist)

## Agentic Workflow Expectations

### OODA Loop Integration
```
Observe  → Read [SIMPLICITY.md] for philosophical context
Orient   → Consult [PRACTICAL_GUIDE.md] for implementation patterns  
Decide   → Apply sarcasmotron to eliminate vagueness
Act      → Implement with mathematical rigor
```

### MEMENTUM Memory System
The project uses a git-based memory system for capturing critical insights and decisions. See [MEMENTUM.md](./MEMENTUM.md) for complete details.

**Core Operations**:
```
λ store(x) → memories/{symbol}-{date}-{slug}.md → git commit -m "{symbol} x"
λ recall(q,n=2) → git log -n n -- memories/ | git grep -i q
```

**Memory Symbols**:
- 🧠💡 **insight** - Novel architectural insight
- 🔄Δ **pattern-shift** - Significant pattern shift  
- 🎯⚡ **decision** - Strategic decision with >1 week impact
- 🌀 **meta** - Meta-learning that changes approach

### Development Navigation
- **For philosophical foundations**: Refer to [SIMPLICITY.md](./SIMPLICITY.md)
- **For actionable commands and practical guidance**: Refer to [PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md)
- **For memory operations**: Refer to [MEMENTUM.md](./MEMENTUM.md)

---

*Created by opencode agent with nucleus-tutor and brepl.*  
*Integrated with MEMENTUM git memory system*
