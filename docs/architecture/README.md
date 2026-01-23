# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) documenting key architectural decisions in the Simplicity project.

## What is an ADR?

An Architecture Decision Record (ADR) is a document that captures an important architectural decision made along with its context and consequences.

## Format

Each ADR follows this structure:

```markdown
# ADR-XXX: Title

## Status
[Proposed | Accepted | Deprecated | Superseded]

## Context
What is the issue that we're seeing that is motivating this decision or change?

## Decision
What is the change that we're proposing and/or doing?

## Consequences
What becomes easier or more difficult to do because of this change?
```

## Index

- [ADR-001: Polylith Architecture](./001-polylith-architecture.md)
- [ADR-002: SQLite for Persistence](./002-sqlite-persistence.md)
- [ADR-003: Component Separation](./003-component-separation.md)
- [ADR-004: Security-First Design](./004-security-first-design.md)
- [ADR-005: Zero-Downtime Deployment](./005-zero-downtime-deployment.md)

## Creating New ADRs

1. Copy the template: `cp 000-template.md XXX-your-title.md`
2. Fill in the sections
3. Submit for review
4. Update this index

## Principles

- **Document significant decisions**: Not every decision needs an ADR, focus on structural/architectural choices
- **Context is king**: Explain WHY, not just WHAT
- **Immutable**: Once accepted, ADRs are not modified (supersede instead)
- **Lightweight**: Keep it concise (1-2 pages max)
