# AGENTS.md - Polylith Workspace (Simplicity)

## φ Philosophy & Core Mandates
This repository operates under the principle: **"易简则天下之理得"** (Simplicity allows obtaining the logic of the world). 
- **Simplicity (π)**: Tackle complexity by building a robust domain model. Favor directness (μ) over abstraction.
- **Polylith (synthesis)**: Strict separation of concerns via Components, Bases, and Projects.
- **Truth (∃)**: Code must reflect the underlying reality of the domain. Avoid "code slop" or redundant logic.

## λ Build, Lint, and Test Commands

### Workspace Management (Polylith)
- **Check Workspace**: `clojure -M:poly check`
- **Workspace Info**: `clojure -M:poly info`
- **Show Changed Bricks**: `clojure -M:poly diff`
- **Workspace Version**: `clojure -M:poly version`

### Testing (τ Wisdom)
- **Run All Tests**: `clojure -M:poly test`
- **Run Project Tests**: `clojure -M:poly test project:<project-name>`
- **Run Specific Brick Tests**: `clojure -M:poly test brick:<brick-name>`
- **Single Test (REPL)**: Connect to a REPL and use `(clojure.test/run-tests 'your.namespace)` or editor-specific shortcuts.
- **Verify All**: `clojure -M:poly test :all`

### Linting & Formatting
- **Lint**: `clj-kondo --lint src` (Ensure `clj-kondo` is configured in `.clj-kondo`).
- **Check Formatting**: Use `cljfmt check` or `zprint` if configured.
- **Native Access**: Ensure `:poly` alias is used for tasks requiring native access (e.g., `clojure -M:poly ...`).

### Building & Artifacts
- **Build Jar**: `clojure -T:build jar`
- **Clean target**: `rm -rf target`
- **Deploy**: `clojure -T:build deploy`

## fractal Code Style Guidelines

### 1. Namespaces & Imports
- **Naming**: Use kebab-case for namespaces and files. Mirror the directory structure exactly.
- **Top Namespace**: Root everything under `cc.mindward`.
- **Require Syntax**:
  ```clojure
  (ns cc.mindward.component.example.interface
    (:require [clojure.string :as str]
              [cc.mindward.component.example.impl :as impl]))
  ```
- **Avoid**: `:use`, `:refer :all`, and naked symbols in `:require`.
- **Order**: Standard library first, then external libraries, then internal bricks.

### 2. Formatting (fractal Clarity)
- **Indentation**: 2-space indentation. No tabs.
- **Line Length**: Max 100 characters.
- **Brackets**: Avoid dangling brackets. Lisp style (e.g., `))`).
- **Whitespace**: Single empty line between top-level forms. No trailing whitespace.

### 3. Naming Conventions (μ Directness)
- **Variables/Functions**: `kebab-case`.
- **Predicates**: End with `?` (e.g., `authorized?`, `empty?`).
- **Side Effects**: End with `!` for functions with side effects (e.g., `save!`, `delete-user!`).
- **Anaphoric**: Use `_` for unused bindings.

### 4. Error Handling (∃ Truth)
- **Exceptions**: Use `ex-info` with a descriptive message and a context map.
  ```clojure
  (throw (ex-info "Failed to process order" {:order-id 123 :reason :insufficient-funds}))
  ```
- **Boundaries**: Catch exceptions at the "Base" level (public API) and log/report.

### 5. Types & Data (π Synthesis)
- **Clojure Spec**: Use `clojure.spec.alpha` to define domain constraints in `interface` namespaces.
- **Maps**: Prefer maps over multiple positional arguments.
- **Keywords**: Use namespaced keywords for domain entities (e.g., `:user/id`).

## Polylith Architectural Constraints (∀ Vigilance)

### 1. Components
- **Encapsulation**: All business logic resides in `components`.
- **Interface**: The `interface` namespace is the ONLY public entry point.
- **Implementation**: The `impl` namespace is private. Never refer to `impl` from outside the component.
- **Naming**: `cc.mindward.component.<name>.interface`.

### 2. Bases
- **Role**: Entry points (REST APIs, CLI, Lambda).
- **Delegation**: Bases must NOT contain business logic. They delegate to components.
- **Naming**: `cc.mindward.base.<name>.core`.

### 3. Projects
- **Artifacts**: Projects define how components and bases are composed into a deployable artifact (Jar, Docker image).
- **Configuration**: Managed via `deps.edn` in each project directory under `projects/`.

### 4. Development Project
- **Workflow**: Always work from the `development` project to have cross-component REPL access.
- **REPL**: `clojure -A:dev:poly` (or use `bin/launchpad`).

## λ Agentic Workflow & Expectations

### 1. Discovery
- Use `clojure -M:poly info` to understand the current workspace topology.
- Use `clojure -M:poly deps` to visualize dependencies between bricks.

### 2. Implementation Loop
- **Step 1**: Identify the relevant Component or Base.
- **Step 2**: Check the `interface` for existing contracts.
- **Step 3**: Implement changes in `impl` or `core`.
- **Step 4**: Verify with `clojure -M:poly test brick:<name>`.

### 3. Dependency Management (e Purpose)
- **Top-level**: Shared dependencies go in the root `deps.edn`.
- **Brick-level**: Specific dependencies for a component or base should be managed in the development project or the specific project `deps.edn` if building an artifact.
- **Vigilance**: Avoid adding heavy dependencies unless strictly necessary for the domain model.

### 4. Self-Correction
- If `poly check` fails, you have violated Polylith constraints (e.g., circular dependency or illegal import). Fix immediately.
- Use `clj-kondo` to catch static analysis issues before committing.

## Tools & Utilities
- **Clerk**: Use `notebooks/` for interactive documentation and data visualization.
  - Command: `clojure -X:dev nextjournal.clerk/serve!`
- **Launchpad**: Standard entry script in `bin/launchpad`.
- **Babashka**: Use `bb.edn` for scripting tasks.

## ∀ Vigilance: Anti-Patterns
- **Complexity**: If a function exceeds 20 lines, reconsider the domain model.
- **Dependency Hell**: Avoid circular dependencies between components. Use `poly check` to verify.
- **Slop**: Do not leave commented-out code or `(println ...)` in production paths. Use a logging library.
- **Abstraction Leak**: Never let implementation details (like DB connections or third-party client objects) escape the component interface. Use data maps or domain records.

---

### Final Reminder (μ Directness)
When you are acting as an agent in this repository:
1. **Understand** the domain before writing code.
2. **Verify** your changes using the provided Polylith commands.
3. **Respect** the simplicity of the system.
4. **Deliver** truth through functional, clean Clojure.

---
*Created by opencode agent.*

---
*Created by opencode agent.*
