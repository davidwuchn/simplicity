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
- **[security.md](./security.md)** - Security controls and testing (160 security assertions)
- **[deployment-cloudflare.md](./deployment-cloudflare.md)** - Production deployment guide

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

---

**Philosophy**: 易简则天下之理得 (Simplicity allows obtaining the logic of the world)

**Last Updated**: 2024-05-20
