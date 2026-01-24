# Documentation Index

This directory contains comprehensive documentation for the Simplicity project.

## 📚 Core Documentation

### Getting Started
- **[../README.md](../README.md)** - Project overview and quick start
- **[hot-reload.md](./hot-reload.md)** - **★ Hot reload development guide** (0.5s feedback loop!)

### Development
- **[clojure-mcp-integration.md](./clojure-mcp-integration.md)** - AI assistant integration (Claude Desktop/Code)
- **[../.clojure-mcp/QUICK_START.md](../.clojure-mcp/QUICK_START.md)** - Quick reference for clojure-mcp

### Architecture & Guidelines
- **[../PROJECT_SUMMARY.md](../PROJECT_SUMMARY.md)** - Complete project overview (optimized for LLMs)
- **[../LLM_CODE_STYLE.md](../LLM_CODE_STYLE.md)** - Coding standards and Polylith constraints
- **[../AGENTS.md](../AGENTS.md)** - AI agent operational guidelines (Eight Keys philosophy)

### API & Deployment
- **[api.md](./api.md)** - REST API reference
- **[security.md](./security.md)** - Security controls and testing (160 security assertions with timing attack prevention)
- **[deployment-cloudflare.md](./deployment-cloudflare.md)** - Production deployment guide

### Testing & Quality
- **Enhanced Test Infrastructure** - Property-based, documentation, performance, and security tests
- **652 Assertions** - Comprehensive test coverage with real database integration

## 🚀 Quick Start Guide

### Development Workflow (Recommended)

```bash
# 1. Start development environment with hot reload
bb dev

# 2. In REPL, start the server
user=> (start)

# 3. Edit code → (restart) in REPL
```

See **[hot-reload.md](./hot-reload.md)** for complete guide.

## 📖 Documentation by Topic

| Document | Description | When to Use |
|----------|-------------|-------------|
| [hot-reload.md](./hot-reload.md) | Hot reload development guide | **Every day** - main workflow |
| [clojure-mcp-integration.md](./clojure-mcp-integration.md) | AI assistant setup | Setting up Claude Desktop/Code |
| [../PROJECT_SUMMARY.md](../PROJECT_SUMMARY.md) | Project overview | Onboarding, LLM context |
| [../LLM_CODE_STYLE.md](../LLM_CODE_STYLE.md) | Coding standards | Before writing code |
| [api.md](./api.md) | REST API reference | Building clients |
| [security.md](./security.md) | Security controls | Security review |
| [deployment-cloudflare.md](./deployment-cloudflare.md) | Production deployment | Deploying to production |

### Enhanced Test Infrastructure

The project now includes comprehensive test types aligned with the Eight Keys philosophy:

| Test Type | Command | Purpose |
|-----------|---------|---------|
| **Property-based Tests** | `bb test:property` | Generative testing with `test.check` for mathematical properties |
| **Documentation Tests** | `bb test:documentation` | Verify code behavior matches documented contracts |
| **Performance Tests** | `bb test:performance` | Measure evolution speed, memory usage, concurrent access |
| **Security Tests** | `bb test:user` (security) | Timing attack prevention, SQL injection, XSS, CSRF |

---

**Philosophy**: 易简则天下之理得 (Simplicity allows obtaining the logic of the world)

**Test Coverage**: 652 assertions with enhanced test infrastructure

**Last Updated**: 2024-11-23
