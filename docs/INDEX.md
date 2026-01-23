# Documentation Index

This directory contains comprehensive documentation for the Simplicity project.

## 📚 Core Documentation

### Getting Started
- **[../README.md](../README.md)** - Project overview and quick start
- **[hot-reload-workflow.md](./hot-reload-workflow.md)** - **★ Hot reload development guide** (0.5 second feedback loop!)
- **[hot-reload-best-practices.md](./hot-reload-best-practices.md)** - **Best practices for hot reload** (lifecycle, state preservation, etc.)
- **[hot-reload-implementation.md](./hot-reload-implementation.md)** - Implementation details of hot reload

### Development
- **[clojure-mcp-integration.md](./clojure-mcp-integration.md)** - AI assistant integration (Claude Desktop/Code)
- **[../.clojure-mcp/QUICK_START.md](../.clojure-mcp/QUICK_START.md)** - Quick reference for clojure-mcp
- **[../.clojure-mcp/INTEGRATION_STATUS.md](../.clojure-mcp/INTEGRATION_STATUS.md)** - Integration verification report

### Architecture & Guidelines
- **[../PROJECT_SUMMARY.md](../PROJECT_SUMMARY.md)** - Complete project overview (optimized for LLMs)
- **[../LLM_CODE_STYLE.md](../LLM_CODE_STYLE.md)** - Coding standards and Polylith constraints
- **[../AGENTS.md](../AGENTS.md)** - AI agent operational guidelines (Eight Keys philosophy)

### API & Deployment
- **[api.md](./api.md)** - REST API reference
- **[security.md](./security.md)** - Security controls and testing (501 assertions)
- **[deployment-cloudflare.md](./deployment-cloudflare.md)** - Production deployment guide

## 🚀 Quick Start Guide

### Development Workflow (Recommended)

```bash
# 1. Start development environment with hot reload
bb dev

# 2. In REPL, start the server
user=> (start)

# 3. Open browser
# http://localhost:3000

# 4. Edit code in components/bases

# 5. Hot reload (0.5 seconds!)
user=> (restart)

# 6. Test changes in browser
```

See **[hot-reload-workflow.md](./hot-reload-workflow.md)** for complete guide.

### AI Assistant Integration

```bash
# Start development environment
bb dev
user=> (start)

# In another terminal, use Claude Code
claude

# AI can now:
# - Edit code via clojure_edit
# - Test in REPL via clojure_eval  
# - Hot reload: (restart)
```

See **[clojure-mcp-integration.md](./clojure-mcp-integration.md)** for setup.

## 📖 Documentation by Topic

### 🛠 Development

| Document | Description | When to Use |
|----------|-------------|-------------|
| [hot-reload-workflow.md](./hot-reload-workflow.md) | Hot reload development guide | **Every day** - main workflow |
| [hot-reload-best-practices.md](./hot-reload-best-practices.md) | Best practices for hot reload | Writing components, understanding lifecycle |
| [hot-reload-implementation.md](./hot-reload-implementation.md) | How hot reload works | Understanding internals |
| [clojure-mcp-integration.md](./clojure-mcp-integration.md) | AI assistant setup | Setting up Claude Desktop/Code |
| [../.clojure-mcp/QUICK_START.md](../.clojure-mcp/QUICK_START.md) | Quick reference for MCP | Daily AI workflow |

### 🏗 Architecture

| Document | Description | When to Use |
|----------|-------------|-------------|
| [../PROJECT_SUMMARY.md](../PROJECT_SUMMARY.md) | Project overview | Onboarding, LLM context |
| [../LLM_CODE_STYLE.md](../LLM_CODE_STYLE.md) | Coding standards | Before writing code |
| [../AGENTS.md](../AGENTS.md) | AI agent guidelines | AI-assisted development |

### 🔧 API & Operations

| Document | Description | When to Use |
|----------|-------------|-------------|
| [api.md](./api.md) | REST API reference | Building clients, integration |
| [security.md](./security.md) | Security controls | Security review, compliance |
| [deployment-cloudflare.md](./deployment-cloudflare.md) | Production deployment | Deploying to production |

## 🎯 Common Tasks

### Task: Start Developing
1. Read: [hot-reload-workflow.md](./hot-reload-workflow.md)
2. Run: `bb dev`
3. In REPL: `(start)`
4. Edit code → `(restart)` → Test

### Task: Set Up AI Assistant
1. Read: [clojure-mcp-integration.md](./clojure-mcp-integration.md)
2. Configure Claude Desktop or Claude Code
3. Run: `bb dev` → `(start)`
4. AI can now edit and test code

### Task: Deploy to Production
1. Read: [deployment-cloudflare.md](./deployment-cloudflare.md)
2. Review: [security.md](./security.md)
3. Run tests: `clojure -M:poly test :dev`
4. Build: `./scripts/build-deployment.sh`

### Task: Understand Architecture
1. Read: [../PROJECT_SUMMARY.md](../PROJECT_SUMMARY.md)
2. Read: [../LLM_CODE_STYLE.md](../LLM_CODE_STYLE.md)
3. Run: `clojure -M:poly info`

### Task: Troubleshoot Issues
1. Check: [hot-reload-workflow.md#troubleshooting](./hot-reload-workflow.md#troubleshooting)
2. Check: [clojure-mcp-integration.md#troubleshooting](./clojure-mcp-integration.md#troubleshooting)
3. Verify: `clojure -M:poly check`

## 📝 Documentation Standards

All documentation follows these principles:

- **φ (Vitality)**: Living docs that evolve with the codebase
- **fractal (Clarity)**: Clear, unambiguous instructions
- **μ (Directness)**: Get to the point quickly
- **π (Synthesis)**: Complete mental models, not fragments

### For Developers
- Use hot reload workflow for instant feedback
- Follow Polylith constraints (see [../LLM_CODE_STYLE.md](../LLM_CODE_STYLE.md))
- Run `clojure -M:poly check` before committing

### For AI Assistants
- Load [../PROJECT_SUMMARY.md](../PROJECT_SUMMARY.md) for context
- Follow [../LLM_CODE_STYLE.md](../LLM_CODE_STYLE.md) for standards
- Use [../AGENTS.md](../AGENTS.md) for operational guidelines

## 🔗 External Resources

- **Polylith**: https://polylith.gitbook.io/
- **Clojure-MCP**: https://github.com/bhauman/clojure-mcp
- **Clojure**: https://clojure.org/
- **Ring**: https://github.com/ring-clojure/ring
- **Reitit**: https://github.com/metosin/reitit

## 🆘 Getting Help

1. **Development Issues**: See [hot-reload-workflow.md#troubleshooting](./hot-reload-workflow.md#troubleshooting)
2. **AI Integration**: See [clojure-mcp-integration.md#troubleshooting](./clojure-mcp-integration.md#troubleshooting)
3. **Security Questions**: See [security.md](./security.md)
4. **Deployment Issues**: See [deployment-cloudflare.md](./deployment-cloudflare.md)

---

**Philosophy**: 易简则天下之理得 (Simplicity allows obtaining the logic of the world)

**Last Updated**: 2026-01-23
