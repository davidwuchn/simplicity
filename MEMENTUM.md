# MEMENTUM — git memory

> **VIOLATION: ∃ (Truth)**  
> Symbolic notation without mathematical grounding is abstract nonsense.  
> **Correct**: Reference [SIMPLICITY.md](./SIMPLICITY.md) for mathematical mapping of Eight Keys symbols.

## Nucleus Operating Principles

```
[phi fractal euler tao pi mu] | [Δ λ ∞/0 | ε/φ Σ/μ c/h] | OODA
Human ⊗ AI
```

**Mathematical Grounding** (see [SIMPLICITY.md](./SIMPLICITY.md)):
- **φ (Vitality)**: Euler's Formula $e^{iπ} + 1 = 0$
- **fractal (Clarity)**: Euler's Polyhedra Formula $V - E + F = 2$
- **e (Purpose)**: Exponential Growth $e^x$
- **τ (Wisdom)**: Calculus $\frac{df}{dt} = \lim_{h\to0} \frac{f(t+h)-f(t)}{h}$
- **π (Synthesis)**: Pythagorean Theorem $a^2 + b^2 = c^2$
- **μ (Directness)**: Logarithms $\log xy = \log x + \log y$
- **∃ (Truth)**: Information Theory $H = -\sum p(x) \log p(x)$
- **∀ (Vigilance)**: Chaos Theory $x_{t+1} = k x_t (1 - x_t)$

## λ Operations

```
λ store(x) → memories/{symbol}-{date}-{slug}.md → git commit -m "{symbol} x"
λ recall(q,n=2) → git log -n n -- memories/ | git grep -i q
```

Default depth n=2 (fibonacci hint: n-1, n-2). Adjust n for deeper search.

## Symbols

```
🧠💡 insight  🔄Δ pattern-shift  🎯⚡ decision  🌀 meta
```

## Auto-Trigger

**Store ONLY when critical:**

- Novel architectural insight (🧠💡)
- Significant pattern shift (🔄Δ)
- Strategic decision with >1 week impact (🎯⚡)
- Meta-learning that changes approach (🌀)

**Skip:** routine changes, minor fixes, incremental work

File: `memories/{symbol}-YYYY-MM-DD-{slug}.md` (keep <200 tokens)
Commit: `git commit -m "{symbol} terse-description"`

**Token Budget:** Each memory ≤200 tokens. Compress ruthlessly.

## Recall Pattern

```bash
git log -n 2 -- memories/           # Recent context (n-1, n-2)
git grep -i "{query}" memories/     # Semantic search all memories
git log --grep "{symbol}" -- memories/  # Search by symbol
```

Fibonacci depth: Start shallow (2), expand as needed (3,5,8,13...)

## OODA

```
observe  → git log -n 13 -- memories/
orient   → git grep -i "{query}" memories/
decide   → create|update memory
act      → git commit
```

---

repo=memory | commits=timeline | git=database

**Document Navigation**:
- **Philosophy & Eight Keys**: [SIMPLICITY.md](./SIMPLICITY.md)
- **Implementation Guide**: [PRACTICAL_GUIDE.md](./PRACTICAL_GUIDE.md)  
- **Agentic Workflow**: [AGENTS.md](./AGENTS.md)

[phi fractal euler tao pi mu] | [Δ λ ∞/0 | ε/φ Σ/μ c/h] | OODA
Human ⊗ AI
