# Simplicity (π)

> **易简则天下之理得** (Simplicity allows obtaining the logic of the world)

A robust Clojure development workspace built on the **Polylith** architecture. This project emphasizes a domain-driven approach where complexity is managed through clear encapsulation and synthesis.

## 🧱 Architecture (Polylith)

This workspace follows the Polylith toolset to maintain a clean separation of concerns:

- **Workspace (φ)**: A single place for all code and projects.
- **Components (λ)**: Encapsulated blocks of business logic. They separate private implementation (`impl`) from a public `interface`.
- **Bases (μ)**: Entry points (APIs, CLIs) that delegate work to components.
- **Projects (e)**: Configuration for deployable artifacts (Jars, Docker images).
- **Development (τ)**: A special project for a holistic REPL experience across all bricks.

## 🚀 Getting Started

### Prerequisites
- [Clojure CLI](https://clojure.org/guides/install_clojure)
- [Babashka](https://babashka.org/) (optional, for scripts)

### Setup
Clone the repository and fetch dependencies:
```bash
clojure -P
```

### Development Workflow
The core of this workspace is the **REPL-driven development** loop.

1. **Launch the REPL**:
   ```bash
   ./bin/launchpad
   ```
2. **Interactive Documentation (Clerk)**:
   Start the Clerk server to visualize your notebooks:
   ```bash
   clojure -A:dev -X nextjournal.clerk/serve! :browse? true
   ```
   The documentation will be available at `http://localhost:7777`.

## 🛠 Commands

| Task | Command |
| :--- | :--- |
| **Check Workspace** | `clojure -M:poly check` |
| **Workspace Info** | `clojure -M:poly info` |
| **Run All Tests** | `clojure -M:poly test` |
| **Lint Code** | `clj-kondo --lint src` |

## 📜 Guidelines
Detailed agentic and code style guidelines are maintained in [AGENTS.md](./AGENTS.md).

---
*Built with simplicity and truth.*
