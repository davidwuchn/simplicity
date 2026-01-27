# ADR-001: Polylith Architecture

**Date**: 2026-01-22  
**Status**: Accepted  
**Author**: Simplicity Team

## Context

**Problem Statement:**

Building a maintainable Clojure web application that allows for rapid iteration, easy testing, and clear separation of concerns while avoiding the typical monolithic application pitfalls.

**Current Situation:**

Traditional Clojure project structures often lead to:
- Tight coupling between business logic and infrastructure
- Difficult-to-test code due to mixed concerns
- Code reuse challenges across projects
- Unclear boundaries between modules

**Constraints:**
- Must support both development and production deployments
- Need clear component boundaries for team collaboration
- Require ability to test components in isolation
- Want to enable potential code reuse across future projects

**Assumptions:**
- Team is familiar with Clojure
- Development focuses on a single application initially
- May expand to multiple services/applications in future

## Decision

**Chosen Solution:**

Adopt the **Polylith architecture** pattern for organizing the codebase.

**Structure:**
```
workspace/
├── components/          # Reusable business logic
│   ├── auth/
│   ├── game/
│   ├── user/
│   └── ui/
├── bases/              # Entry points (REST API, CLI, etc.)
│   └── web-server/
├── projects/           # Deployable artifacts
│   └── development/
└── development/        # REPL-driven development
```

**Rationale:**

1. **Clear Boundaries**: Components define explicit interfaces, preventing accidental coupling
2. **Testability**: Each component can be tested in isolation with its own test suite
3. **Reusability**: Components can be shared across multiple projects/bases
4. **REPL Development**: The development project provides cross-component REPL access
5. **Validated Design**: `poly check` ensures architectural constraints are maintained

**Alternatives Considered:**

1. **Traditional Monolith (single namespace hierarchy)**
   - Pros: Simpler initial setup, familiar pattern
   - Cons: Coupling increases over time, harder to refactor, testing becomes difficult
   - Why rejected: Doesn't scale well for maintainability

2. **Microservices from Day 1**
   - Pros: Maximum isolation, independent deployment
   - Cons: Operational complexity, network overhead, premature optimization
   - Why rejected: Overkill for current application size, can evolve to this later if needed

3. **Multi-Module Maven/Leiningen Project**
   - Pros: Some modularity, existing tooling
   - Cons: Build complexity, less flexible than Polylith, no REPL integration
   - Why rejected: Polylith provides better developer experience

## Consequences

**Positive:**
- **Enforced Modularity**: `poly check` prevents circular dependencies and violations
- **Fast Feedback**: Can run tests for specific components/bricks
- **Clear Mental Model**: Interface vs Implementation separation is explicit
- **Team Collaboration**: Clear ownership boundaries for components
- **Future-Proof**: Easy to extract components into separate services if needed

**Negative:**
- **Learning Curve**: Team needs to understand Polylith concepts (components, bases, projects)
- **More Files**: More namespace files compared to traditional structure
- **Tooling Dependency**: Requires `poly` CLI tool for workspace management

**Neutral:**
- **Directory Depth**: More nested structure (components/name/src/namespace)
- **Build Configuration**: Need to manage `deps.edn` at multiple levels

**Risks:**
- **Team Adoption**: Developers unfamiliar with Polylith may find it confusing initially
- **Polylith Tool Maintenance**: Dependency on external tool (mitigated: active community)

**Mitigation:**
- **Documentation**: Maintain PRACTICAL_GUIDE.md with clear Polylith guidelines
- **Code Reviews**: Enforce architectural constraints during reviews
- **Onboarding**: Provide Polylith training for new team members
- **Tooling**: Configure IDE/editor support for Polylith structure

## Implementation

**Changes Required:**
1. ✅ Organize code into components: auth, game, user, ui
2. ✅ Create web-server base for HTTP entry point
3. ✅ Set up development project with all bricks included
4. ✅ Configure `poly` CLI tool
5. ✅ Add `poly check` to CI/CD pipeline

**Testing Strategy:**
- Each component has its own test suite in `component/test/`
- Integration tests in base (web-server)
- `poly test :dev` runs all tests with cross-component access

**Rollout Plan:**
- ✅ Phase 1: Migrate existing code to Polylith structure
- ✅ Phase 2: Add `poly check` to pre-commit hooks
- ✅ Phase 3: Document architecture in PRACTICAL_GUIDE.md & AGENTS.md
- ⏳ Phase 4: Create production project (when needed)

## Related Documents

- [Polylith Documentation](https://polylith.gitbook.io/)
- [AGENTS.md](../../AGENTS.md) - Workspace navigation & three-document hierarchy
- [ADR-003: Component Separation](./003-component-separation.md)

## Notes

**Key Polylith Principles Applied:**
- **Interface Namespace**: Public API (e.g., `cc.mindward.user.interface`)
- **Implementation Namespace**: Private (e.g., `cc.mindward.user.impl`)
- **No Cross-Component Impl Access**: Components only depend on other component interfaces
- **Bases are Thin**: Delegation to components, no business logic

**Verified with**: `clojure -M:poly check` (passes)
