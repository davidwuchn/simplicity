# Web Server Base

HTTP server entry point for the Simplicity application. Built with Ring and Reitit, this base provides session-based authentication, CSRF protection, and a JSON API for the Game of Life interface.

## Architecture

**Role**: Entry point (Base)  
**Dependencies**: auth, user, game, ui components  
**Protocol**: HTTP/1.1 with cookie-based sessions  
**Port**: 3000 (configurable via `PORT` env var)

## Routing Table

| Method | Path | Handler | Auth Required | Description |
|--------|------|---------|---------------|-------------|
| GET | `/` | `landing-page` | No | Landing page, redirects authenticated users to `/game` |
| GET | `/login` | `login-page` | No | Login form |
| POST | `/login` | `handle-login` | No | Process login credentials |
| GET | `/signup` | `signup-page` | No | Signup form |
| POST | `/signup` | `handle-signup` | No | Create new user account |
| GET | `/logout` | `handle-logout` | No | End session and redirect to `/login` |
| GET | `/leaderboard` | `leaderboard-page` | No | Global leaderboard view |
| GET | `/game` | `game-page` | **Yes** | Main game interface |
| POST | `/game/score` | `save-score` | **Yes** | Update user high score |
| POST | `/api/game` | `game-api` | **Yes** | Game state manipulation (create/evolve/save/load) |
| GET | `/api/games` | `list-saved-games-api` | **Yes** | List all saved games |

## Quick Reference

**Starting the server:**
```bash
clojure -M -m cc.mindward.web-server.core
```

**Key endpoints:**
- Landing: http://localhost:3000/
- Game: http://localhost:3000/game (auth required)
- API: POST http://localhost:3000/api/game

**See full documentation:** [API Reference](../../docs/api.md)

## Middleware Stack

Uses `ring-defaults` site-defaults:
- Session management (cookie-based)
- CSRF protection (anti-forgery)
- Parameter parsing (form params keywordized)
- Static file serving (`resources/public/`)
- Security headers

## Testing

```bash
clojure -M:poly test :dev
```

See `test/cc/mindward/web_server/core_test.clj` for integration tests.

## See Also

- [API Documentation](../../docs/api.md) - Full REST API reference
- [Architecture](../../docs/architecture.md) - System design
- [Reitit](https://github.com/metosin/reitit) - Routing library
- [Ring](https://github.com/ring-clojure/ring) - Web application library
