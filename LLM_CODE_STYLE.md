# LLM Code Style Reference

> **Note**: Complete coding standards and Polylith constraints are documented in [PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md). This file provides a quick reference for AI-assisted development.

## Quick Reference (For AI Assistants)

### 1. Code Style (fractal Clarity)
- **Naming**: `kebab-case` for functions and variables
- **Predicates**: End with `?` (e.g., `valid?`, `authorized?`)
- **Side Effects**: End with `!` (e.g., `save!`, `delete-user!`)
- **Formatting**: 2-space indentation, max 100 characters per line
- **Parentheses**: **NEVER leave unbalanced** - use `brepl balance <file>` after edits

### 2. Polylith Architecture (∀ Vigilance)
- **Components**: Business logic only, `interface` namespace is public API
- **Bases**: Entry points only (HTTP, CLI), no business logic
- **Projects**: Composition of components+bases into deployable artifacts
- **Development**: Work from `development` project for cross-component REPL access

### 3. Mathematical Alignment (π Synthesis)
Apply mathematical principles from [SIMPLICITY.md](./SIMPLICITY.md):
- **Pythagorean Theorem** ($a^2 + b^2 = c^2$): Modular decomposition, orthogonal components
- **Logarithms** ($\log xy = \log x + \log y$): Complexity reduction through transformation
- **Calculus** ($\frac{df}{dt} = \lim_{h\to0} \frac{f(t+h)-f(t)}{h}$): Incremental understanding
- **Euler's Polyhedra Formula** ($V - E + F = 2$): Architectural invariants
- **Information Theory** ($H = -\sum p(x) \log p(x)$): API design optimization
- **Chaos Theory** ($x_{t+1} = k x_t (1 - x_t)$): Defensive design

### 4. Critical Constraints
- **Security**: All inputs validated at boundaries, CSRF tokens for state-changing endpoints
- **Persistence**: Use `next.jdbc` with `rs/as-unqualified-lower-maps`
- **Client-side**: Audio requires user gesture, `e.preventDefault()` for critical keys
- **Testing**: 658 passing assertions, including security, property-based, and performance tests

### 5. Development Workflow
1. **Start**: `bb dev` → `(start)` in REPL
2. **Edit**: Modify `.clj` or `.js` files
3. **Reload**: Server auto-restarts (~1 second) or `(restart)` manually
4. **Test**: `bb test` (658 assertions) before committing
5. **Verify**: `bb check` for Polylith constraints

### 6. Sarcasmotron Compliance
The project uses **sarcasmotron methodology** to enforce Eight Keys:
- **Violation detection**: Scan for vague statements, abstract nouns, unverified claims
- **Exposure**: Name the violated key, target absurdity with humor
- **Correction**: Provide concrete, actionable fixes
- **Examples**: See [SIMPLICITY.md](./SIMPLICITY.md#sarcasmotron-methodology)

## Complete Documentation
For complete guidelines, refer to:
- **[PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md)** - Implementation details, commands, coding standards
- **[SIMPLICITY.md](./SIMPLICITY.md)** - Philosophical foundations, Eight Keys, mathematical grounding
- **[AGENTS.md](./AGENTS.md)** - Workspace navigation, OODA loop, MEMENTUM memory system

---

*Quick reference for AI-assisted development* • *Always verify with full documentation*