# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Documentation Overhaul (Simplicity π)**
  - Consolidated hot reload guides into a single authoritative `docs/hot-reload.md`.
  - Refactored `PROJECT_SUMMARY.md` into a concise mental model for LLMs.
  - Standardized assertion counts across all documents to current truth (652 total, 160 security-focused).
  - Modernized `ONBOARDING.md`, `TROUBLESHOOTING.md`, and `CONTRIBUTING.md` to favor the `bb dev` workflow.
  - Updated `development/src/user.clj` REPL banner with accurate test stats and emoji-driven clarity.
  - Standardized "Last Updated" dates and fixed future-dated entries.
- **Developer Experience**
  - REPL startup banner with quick reference commands and emoji-organized categories
  - `(banner)` command to redisplay help anytime
  - Better error messages when calling `(restart)` without running system
- **Clojure-MCP Integration**
  - nREPL server configuration (`:nrepl` alias on port 7888)
  - `.clojure-mcp/config.edn` with security boundaries and tool settings
  - `PROJECT_SUMMARY.md` - LLM-optimized project overview for AI context
  - `LLM_CODE_STYLE.md` - Coding standards and Polylith constraints for AI assistants
  - `docs/clojure-mcp-integration.md` - Complete integration guide for Claude Desktop, Claude Code, and other MCP clients
  - `.clojure-mcp/README.md` - Quick reference for clojure-mcp usage
  - Support for AI-assisted development with REPL access and structure-aware editing
- CONTRIBUTING.md with comprehensive contribution guidelines
- LICENSE file (MIT License)
- GitHub Actions CI/CD workflow
- CHANGELOG.md for version tracking

### Changed
- **Code Quality**
  - Refactored `ui/styles.clj` to use centralized color palette (eliminated 40+ hardcoded hex values)
  - Added `c()` helper function for type-safe color lookups with DRY principle
  - Improved theme maintainability - single source of truth for all colors

### Fixed
- Missing `clojure.string` require in `user/validation.clj` (critical namespace error)
- Unused bindings in `web-server/core.clj` and `user/impl.clj`
- Broken `bb lint` task (shell glob expansion issue)
- Stale test count documentation (611 → 618 assertions)

## [1.0.0] - 2026-01-22

### Added
- **Deployment Infrastructure**
  - Build system with tools.build for uberjar compilation (45MB standalone JAR)
  - Multi-stage Dockerfile with optimized caching
  - Docker Compose configuration with persistent volumes
  - Interactive build script (`scripts/build-deployment.sh`)
  - Comprehensive deployment guide for Cloudflare (`docs/deployment-cloudflare.md`)
  - Three deployment options: VPS+Docker, Cloudflare Tunnel, standalone uberjar

- **Security Hardening**
  - Security middleware stack (`bases/web-server/src/cc/mindward/web_server/security.clj`)
  - Rate limiting with token bucket algorithm (login: 5/min, signup: 3/min)
  - Security headers (CSP, X-Frame-Options, HSTS, X-Content-Type-Options, etc.)
  - Input validation at API boundary (username, password, score)
  - Password security: bcrypt + sha512 with timing attack resistance
  - SQL injection prevention with parameterized queries
  - XSS prevention with HTML escaping + CSP headers
  - Comprehensive security tests (501 total assertions)
  - Security documentation (`docs/security.md`)

- **Monitoring & Operations**
  - Health check endpoint (`/health`) with database connectivity check
  - Request/response logging middleware
  - Structured logging with Logback
  - Environment-based log levels (DEBUG, INFO, WARN, ERROR)

- **Documentation**
  - Complete API documentation (`docs/api.md`)
  - Architecture documentation (`docs/architecture.md`)
  - Security controls documentation (`docs/security.md`)
  - Cloudflare deployment guide (`docs/deployment-cloudflare.md`)
  - AI agent operational guidelines (`AGENTS.md`)

- **Testing**
  - 618 passing assertions across test suite
  - Component breakdown:
    - Auth: 3 tests, 25 assertions
    - Game: 13 tests, 136 assertions
    - UI: 70 tests, 267 assertions
    - User: 12 tests, 49 assertions (includes SQL injection tests)
    - Web-server: 37 tests, 177 assertions (includes security tests)

### Changed
- Password minimum length increased from 6 to 8 characters
- Updated all documentation with latest security and deployment information
- Improved error handling with structured logging

### Fixed
- Logback configuration conditional syntax errors
- Web server test failures (11 issues resolved)
- Session cookie security attributes (HttpOnly, Secure, SameSite)

### Security
- Added rate limiting to prevent brute force attacks
- Implemented comprehensive input validation
- Added security headers to all responses
- Enabled HSTS support (configurable via ENABLE_HSTS)
- Fixed timing attack vulnerabilities in password verification

## [0.9.0] - 2025-12-15

### Added
- Initial Polylith workspace structure
- Core components:
  - `auth` - User authentication with bcrypt
  - `user` - User management and SQLite persistence
  - `game` - Conway's Game of Life engine
  - `ui` - Hiccup-based HTML rendering
- Web server base with Ring/Reitit
- Musical mapping system for cellular automata
- Leaderboard and high score tracking
- Session-based authentication
- CSRF protection
- Game save/load functionality

### Features
- Conway's Game of Life simulation
- Real-time Web Audio synthesis
- Pattern recognition (glider, blinker, etc.)
- Cyberpunk aesthetic UI design
- Competitive scoring system

---

## Version History

- **1.0.0** - Production-ready release with deployment and security hardening
- **0.9.0** - Initial release with core features

---

## Links

- [GitHub Repository](https://github.com/davidwuchn/simplicity)
- [Documentation](./README.md)
- [Security Policy](./docs/security.md)
- [Deployment Guide](./docs/deployment-cloudflare.md)
