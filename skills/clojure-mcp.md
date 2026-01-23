# Skill: clojure-mcp

Integrate **clojure-mcp** (Model Context Protocol) workflows with **eca.dev** for advanced Clojure development, leveraging structure-aware editing and REPL-driven development.

## When to use
- When working on complex Clojure projects (especially Polylith).
- When you need to perform structure-aware edits (replacing S-expressions).
- When you want to leverage the REPL for immediate feedback and validation.
- When you need to follow strict architectural constraints like Polylith.

## Instructions

### 1. Environment Setup
Ensure the nREPL server is running. This is the backbone of the integration.
- Command: `clojure -M:nrepl` (Default port: 7888)
- Verify connection using `eca__clj-nrepl-eval` with a simple expression like `(+ 1 2)`.

### 2. Context Loading
Always load the project context before starting complex tasks:
- Read `PROJECT_SUMMARY.md` for architecture.
- Read `LLM_CODE_STYLE.md` for coding standards.
- Read `AGENTS.md` for operational guidelines.

### 3. REPL-Driven Development (RDD)
Before editing files, validate your logic in the REPL:
- Use `eca__clj-nrepl-eval` to test functions, check data structures, and verify assumptions.
- Use `:reload` when requiring namespaces: `(require '[cc.mindward.component.game.interface :as game] :reload)`.

### 4. Structure-Aware Editing
While `eca__edit_file` is text-based, you should think in terms of Clojure forms:
- Identify the exact S-expression to change.
- Use `eca__read_file` to get the context.
- When applying edits, ensure the resulting code is structurally sound.
- **Mandatory**: Run `brepl balance <file>` after any edit to fix parentheses and formatting.

### 5. Polylith Validation
After making changes to components or bases:
- Run `clojure -M:poly check` to ensure no architectural violations (like circular dependencies).
- Run `clojure -M:poly test brick:<brick-name>` to run tests for the modified brick.

## Tools to use

### Evaluation
- `eca__clj-nrepl-eval`: The primary tool for interacting with the running application.
- Use the heredoc pattern for complex code:
  ```bash
  eca__clj-nrepl-eval code: $'(require (quote [cc.mindward.component.game.interface :as game]))\n(game/evolve-grid {})'
  ```
  *(Note: Escape single quotes as \' and use (quote) for symbols/vectors if needed in complex strings)*

### Verification
- `eca__shell_command`:
  - `clojure -M:poly check`
  - `clojure -M:poly test :dev`
  - `brepl balance <path/to/file.clj>`

### Search
- `eca__grep`: Search for usages or patterns across the workspace.

## Examples

### Testing a Component Implementation
```clojure
;; Evaluate in REPL first
(require '[cc.mindward.component.auth.impl :as impl])
(impl/hash-password "secret-password")
```

### Adding a new function to an Interface
1. Read the interface: `eca__read_file path: "components/game/src/cc/mindward/component/game/interface.clj"`
2. Edit the file: `eca__edit_file ...`
3. Balance the file: `eca__shell_command command: "brepl balance components/game/src/cc/mindward/component/game/interface.clj"`
4. Verify: `eca__clj-nrepl-eval code: "(require '[cc.mindward.component.game.interface :as game] :reload)"`

## Polylith Cheat Sheet
- **Components**: `components/<name>/src/cc/mindward/component/<name>/interface.clj` (Public API)
- **Bases**: `bases/<name>/src/cc/mindward/base/<name>/core.clj` (Entry points)
- **Implementation**: `components/<name>/src/cc/mindward/component/<name>/impl.clj` (Private logic)

🤖 Generated with [eca](https://eca.dev)
