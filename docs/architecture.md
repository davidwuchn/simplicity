# Architecture Documentation

## System Overview

**Simplicity** is a musical Game of Life web application that transforms cellular automata patterns into real-time synthesized music. Built with Clojure using the Polylith architecture, it demonstrates clean component separation and domain-driven design.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  HTML/CSS    │  │  JavaScript  │  │  Web Audio   │      │
│  │  (Hiccup)    │  │  Game Loop   │  │  Synthesizer │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                           │ HTTP/JSON
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Web Server (Base)                         │
│         Ring + Reitit + Session + CSRF                       │
└─────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
    ┌─────────┐      ┌─────────┐      ┌─────────┐
    │  Auth   │      │  User   │      │  Game   │
    │Component│      │Component│      │Component│
    └─────────┘      └─────────┘      └─────────┘
                           │                │
                           ▼                ▼
                     ┌──────────┐    ┌──────────┐
                     │ SQLite   │    │ In-Memory│
                     │ Database │    │  Atoms   │
                     └──────────┘    └──────────┘
                     
    ┌─────────┐
    │   UI    │ (Renders HTML for all pages)
    │Component│
    └─────────┘
```

## Polylith Architecture

### Workspace Structure

```
simplicity/
├── components/           # Business logic (reusable)
│   ├── auth/            # Authentication
│   ├── game/            # Game of Life engine
│   ├── user/            # User management
│   └── ui/              # HTML rendering
├── bases/               # Entry points (not reusable)
│   └── web-server/      # HTTP server
├── development/         # Unified REPL project
└── projects/            # (Future: deployment artifacts)
```

### Component Boundaries

**Key Principle**: Components can only depend on other component **interfaces**, never implementations.

```clojure
;; ✅ GOOD: Depend on interface
(require '[cc.mindward.user.interface :as user])
(user/find-by-username "alice")

;; ❌ BAD: Never depend on impl
(require '[cc.mindward.user.impl :as user-impl])  ; FORBIDDEN
```

## Component Details

### Auth Component

**Responsibility**: User authentication

**Public Interface** (`components/auth/src/cc/mindward/auth/interface.clj`):
- `(authenticate username password)` → User map or nil

**Dependencies**:
- `user` component (to fetch user by username)
- `buddy.hashers` (for bcrypt password verification)

**Key Decisions**:
- Password verification only (hashing done in `user` component)
- No session management (delegated to web-server base)
- Returns full user map (excluding `:password_hash`)

**Security**:
- Uses bcrypt for password comparison (constant-time)
- SQL injection prevented via parameterized queries in `user` component

---

### User Component

**Responsibility**: User account management, persistence, leaderboard

**Public Interface** (`components/user/src/cc/mindward/user/interface.clj`):
- `(init!)` → Initialize database schema
- `(create-user! {:username :password :name})` → Create account
- `(find-by-username username)` → Get user record
- `(get-high-score username)` → Get user's high score
- `(update-high-score! username score)` → Update if higher
- `(get-leaderboard)` → Top 50 users by score

**Dependencies**:
- `next.jdbc` (database access)
- `buddy.hashers` (password hashing)

**Database**:
- SQLite (`simplicity.db`)
- Schema:
  ```sql
  CREATE TABLE users (
    username TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    name TEXT NOT NULL,
    high_score INTEGER DEFAULT 0
  );
  ```

**Key Decisions**:
- Passwords hashed with bcrypt (cost factor 11)
- High scores use MAX semantics (only update if higher)
- Leaderboard limited to 50 entries
- No soft deletes (accounts are permanent)

**Performance**:
- Single datasource atom (no connection pooling needed for SQLite)
- Leaderboard query optimized with `ORDER BY` and `LIMIT`

---

### Game Component

**Responsibility**: Conway's Game of Life simulation, pattern recognition, musical event generation

**Public Interface** (`components/game/src/cc/mindward/game/interface.clj`):
- `(initialize!)` → Reset all game state
- `(create-game! game-id [initial-board])` → Create new game
- `(get-board game-id)` → Current cell positions
- `(get-generation game-id)` → Current generation count
- `(evolve! game-id)` → Advance one generation
- `(add-cells! game-id cells)` → Add cells to board
- `(clear-cells! game-id cells)` → Remove cells from board
- `(get-score game-id)` → Calculate current score
- `(get-pattern-analysis game-id)` → Detect known patterns
- `(get-musical-triggers game-id)` → Generate audio events
- `(save-game! game-id name)` → Persist game state
- `(load-game! saved-id new-game-id)` → Restore saved game
- `(list-saved-games)` → List all saved games
- `(delete-game! saved-id)` → Remove saved game

**Dependencies**: None (pure domain logic)

**Data Structures**:
```clojure
;; Active games (in-memory)
@games => {:user-alice-game {:board #{[0 0] [1 1]}
                              :generation 5
                              :created-at timestamp}}

;; Saved games (in-memory, could be persisted)
@saved-games => {"uuid-1" {:board #{...}
                            :generation 5
                            :name "Glider Gun"
                            :saved-at timestamp}}
```

**Game Rules** (Conway's Game of Life):
1. **Survival**: Cell with 2-3 neighbors survives
2. **Birth**: Dead cell with 3 neighbors becomes alive
3. **Death**: All other cells die

**Bounds**: `[-100, -100]` to `[100, 100]` (cells outside bounds are filtered)

**Pattern Recognition**:
Detects known patterns:
- Block (2x2 stable square)
- Beehive (stable hexagon)
- Blinker (period-2 oscillator)
- Toad (period-2 oscillator)
- Glider (moving spaceship)

**Musical Mapping**:
| Trigger | Condition | Audio Event |
|---------|-----------|-------------|
| `density-high` | >50 cells | High-energy chaos (440Hz) |
| `density-mid` | >20 cells | Mid-range rhythm (220Hz) |
| `life-pulse` | Any cells | Pulse rate based on count |
| `drone` | Always | Sustained bass (55Hz) |

**Scoring Algorithm**:
```clojure
complexity-score = max(1, living-cells * min(generation, 100))
stability-bonus = if generation > 10 then 100 else 0
score = complexity-score + stability-bonus
```

**Performance**:
- Board represented as `Set` of `[x y]` coordinates (sparse representation)
- Evolution uses transient collections for speed
- Pattern detection is O(board-size × pattern-count)

**Key Decisions**:
- In-memory state (no database persistence for active games)
- Saved games stored in memory (could be moved to DB)
- Game IDs are keywords (e.g., `:user-alice-game`)
- Bounds prevent infinite board growth

---

### UI Component

**Responsibility**: HTML rendering (Hiccup templates)

**Public Interface** (`components/ui/src/cc/mindward/ui/interface.clj`):
- `(landing-page session)` → Landing page HTML
- `(login-page session params csrf-token)` → Login form
- `(signup-page session params csrf-token)` → Signup form
- `(game-page session csrf-token high-score)` → Game canvas
- `(leaderboard-page session leaderboard)` → Leaderboard table

**Dependencies**:
- `hiccup.core` (HTML generation)

**Design System**:
- **Cyberpunk aesthetic**: Neon colors (#fcee0a yellow, #00f0ff cyan, #ff003c red)
- **Orbitron font**: Google Fonts
- **Tailwind CSS**: Utility-first styling
- **Animations**: CSS glitch effects, text shadows

**Key Decisions**:
- Server-side rendering (no React/Vue)
- CSS embedded in `<style>` tags (no external stylesheets)
- CSRF tokens injected into forms as hidden inputs
- Session-based conditional rendering (show/hide login button)

**Performance**:
- Hiccup is fast (compiles to string concatenation)
- No template caching (rendered on every request)
- Static assets served from `resources/public/`

---

### Web Server Base

**Responsibility**: HTTP entry point, routing, session management

See [bases/web-server/README.md](../bases/web-server/README.md) for full details.

**Key Responsibilities**:
- Route HTTP requests to component interfaces
- Manage cookie-based sessions
- Enforce CSRF protection
- Serve static assets
- Transform component responses to Ring responses

**Dependencies**: ALL components (auth, user, game, ui)

---

## Data Flow

### Example: Login Flow

```
1. Browser → GET /login
   ↓
2. web-server/login-page handler
   ↓
3. ui/login-page (render form with CSRF token)
   ↓
4. HTML response → Browser

5. Browser → POST /login (username, password, CSRF token)
   ↓
6. web-server/handle-login
   ↓
7. auth/authenticate (username, password)
   ↓
8. user/find-by-username (DB query)
   ↓
9. bcrypt password verification
   ↓
10. Return user map or nil
   ↓
11. If success: Create session {:username "alice"}
   ↓
12. Redirect → /game with Set-Cookie header
```

### Example: Game Evolution Flow

```
1. Browser → POST /api/game (action=evolve, CSRF token)
   ↓
2. web-server/game-api handler
   ↓
3. Extract game-id from session username
   ↓
4. game/evolve! :user-alice-game
   ↓
5. Get current board from atom
   ↓
6. Apply Conway's rules (pure function)
   ↓
7. Update atom with new board + incremented generation
   ↓
8. game/get-musical-triggers (analyze new board)
   ↓
9. Return {:board [...] :generation N :triggers [...]}
   ↓
10. JSON encode → Browser
   ↓
11. JavaScript receives response
   ↓
12. Update canvas (redraw cells)
   ↓
13. Web Audio API (trigger synthesizer events)
```

---

## State Management

### Database State (SQLite)

**Location**: `./simplicity.db`

**Tables**:
- `users` (username, password_hash, name, high_score)

**Access Pattern**:
- Single datasource atom in `user` component
- No connection pooling (SQLite is single-writer)
- Transactions via `next.jdbc/execute!`

**Migrations**:
- Schema created on first `user/init!` call
- No migration framework (manual SQL)

### In-Memory State (Atoms)

**Active Games** (`game` component):
```clojure
@games => {game-id {:board #{[x y] ...}
                    :generation N
                    :created-at timestamp}}
```
- **Thread-safety**: Clojure atoms (STM)
- **Persistence**: None (lost on restart)
- **Isolation**: One game per user (game-id based on username)

**Saved Games** (`game` component):
```clojure
@saved-games => {uuid {:board #{...}
                       :generation N
                       :name "..."
                       :saved-at timestamp}}
```
- **Thread-safety**: Clojure atoms
- **Persistence**: None (should be moved to DB)
- **Capacity**: Unlimited (memory leak risk)

**Session State** (Ring middleware):
```clojure
;; Encrypted cookie
{:username "alice"}
```
- **Storage**: Client-side cookie (encrypted)
- **Expiry**: Browser session (no server-side expiry)
- **Size limit**: ~4KB (cookie size limit)

---

## Security Model

### Authentication

**Password Storage**:
- bcrypt hashing (cost factor 11)
- Salts generated automatically by bcrypt
- Hashes stored in `users.password_hash` column

**Session Management**:
- Cookie-based (Ring session middleware)
- HttpOnly flag (prevents XSS cookie theft)
- No SameSite flag (defaults to Lax in modern browsers)
- No Secure flag (HTTP-only in development)

**CSRF Protection**:
- Anti-forgery tokens required for all POST requests
- Tokens stored in session, validated by Ring middleware
- Tokens injected into forms as hidden inputs
- JavaScript must include `X-CSRF-Token` header

### Authorization

**Current Model**:
- Binary: authenticated or not
- No roles or permissions
- Users can only access their own game state (enforced by game-id scheme)

**Future Considerations**:
- Add role-based access control (admin, user, guest)
- Add per-resource permissions (e.g., can edit leaderboard)
- Add API rate limiting per user

### Attack Surface

**Protected**:
- ✅ SQL injection (parameterized queries)
- ✅ Password leakage (bcrypt hashing)
- ✅ CSRF (anti-forgery middleware)
- ✅ XSS (Hiccup auto-escapes HTML)

**Not Protected**:
- ❌ Rate limiting (no limits on API calls)
- ❌ Account enumeration (signup error reveals existence)
- ❌ Session fixation (no session regeneration on login)
- ❌ Brute force (no login attempt limits)
- ❌ Memory exhaustion (unlimited saved games)

---

## Performance Characteristics

### Throughput

**Expected Load**:
- 10-100 concurrent users
- 1-10 requests/second per user
- Mostly API calls (game evolution)

**Bottlenecks**:
- Database: SQLite single-writer (locks on write)
- Game State: STM contention on atom updates
- CPU: Conway's Life evolution (O(n) per generation)

**Scalability Limits**:
- Single-server only (in-memory game state)
- No horizontal scaling (stateful design)
- No caching (every request hits atoms/DB)

### Latency

**Typical Response Times** (development):
- Static pages: <50ms
- API calls (evolve): <100ms
- Database queries: <10ms (SQLite is fast for reads)

**Optimization Opportunities**:
- Cache leaderboard queries (changes infrequently)
- Add lazy loading for saved games list
- Optimize pattern recognition (currently O(n²))

---

## Deployment Architecture

### Current (Development)

```
┌─────────────────┐
│  localhost:3000 │
│   Jetty Server  │
│   (in-process)  │
└─────────────────┘
        │
        ▼
┌─────────────────┐
│  simplicity.db  │
│   (SQLite file) │
└─────────────────┘
```

### Future (Production)

```
┌───────────────┐
│  Load Balancer│
└───────────────┘
    │       │
    ▼       ▼
┌─────┐  ┌─────┐  (Multiple instances - requires session store)
│ App │  │ App │
└─────┘  └─────┘
    │       │
    └───┬───┘
        ▼
┌─────────────────┐
│   PostgreSQL    │  (Shared database)
└─────────────────┘
        ▼
┌─────────────────┐
│   Redis         │  (Session + game state)
└─────────────────┘
```

**Required Changes for Scaling**:
1. Move game state from atoms to Redis
2. Move saved games to database
3. Externalize session store (Redis)
4. Add database connection pooling
5. Add sticky sessions or token-based auth

---

## Testing Strategy

### Unit Tests

**Components**:
- `auth`: Mock `user/find-by-username`
- `user`: Use temporary SQLite database per test
- `game`: Pure functions, no mocks needed
- `ui`: Minimal (Hiccup is simple)

### Integration Tests

**Web Server**:
- Test full HTTP flows (signup → login → game API)
- Test session management
- Test CSRF protection

**See**: `bases/web-server/test/cc/mindward/web_server/core_test.clj`

### End-to-End Tests

**Missing**: No browser automation tests (Selenium, Playwright)

---

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| Language | Clojure | 1.12.4 | Backend logic |
| Runtime | Java | 17+ | JVM runtime |
| Web Server | Jetty | 1.13.0 | HTTP server |
| Routing | Reitit | 0.7.2 | URL routing |
| Middleware | Ring | 0.5.0 | Request/response |
| Database | SQLite | 3.47.1.0 | Persistence |
| DB Client | next.jdbc | 1.3.955 | JDBC wrapper |
| Templating | Hiccup | 2.0.0-RC3 | HTML generation |
| Hashing | Buddy | 2.0.167 | bcrypt passwords |
| JSON | data.json | 2.5.0 | JSON encoding |
| Frontend | Vanilla JS | ES6 | Game loop |
| Audio | Web Audio API | - | Synthesizer |
| CSS | Tailwind CSS | 3.x (CDN) | Styling |
| Build | tools.build | 0.9.2 | JAR builder |
| Testing | clojure.test | builtin | Unit tests |
| Architecture | Polylith | 0.3.32 | Workspace |

---

## Design Decisions & Tradeoffs

### Why Polylith?

**Benefits**:
- Clear component boundaries (prevents spaghetti)
- Reusable components across projects
- Incremental testing (test only changed bricks)
- Unified REPL experience

**Tradeoffs**:
- Steeper learning curve for newcomers
- More boilerplate (interface + impl namespaces)
- Tooling dependency (poly CLI)

### Why SQLite?

**Benefits**:
- Zero-configuration (no server to run)
- Fast for small datasets (<100K users)
- ACID transactions
- File-based (easy backup)

**Tradeoffs**:
- Single-writer (no concurrent writes)
- Not horizontally scalable
- Limited for analytics queries

### Why In-Memory Game State?

**Benefits**:
- Fast access (no DB latency)
- Simple implementation (atoms)
- Natural fit for ephemeral data

**Tradeoffs**:
- Lost on restart (no persistence)
- Not scalable (single-server only)
- Memory leak risk (unlimited saved games)

**Future**: Move to Redis or database

### Why Server-Side Rendering?

**Benefits**:
- Simpler codebase (no build step)
- Faster initial load (no large JS bundle)
- SEO-friendly (though not needed here)

**Tradeoffs**:
- Full page reloads for navigation
- No reactive UI (manual DOM updates in JS)
- Mixed responsibility (server renders, JS updates)

**Future**: Consider htmx for partial updates

---

## Future Enhancements

### Near-Term (Next Sprint)
1. **Persist Saved Games**: Move from atoms to database
2. **Add Game Deletion UI**: Allow users to delete saved games
3. **Fix Session Expiry**: Add server-side expiry (e.g., 30 days)
4. **Add Rate Limiting**: Prevent abuse of API endpoints
5. **Improve Error Messages**: Return structured errors from API

### Medium-Term (Next Quarter)
6. **PostgreSQL Migration**: Replace SQLite for production
7. **Redis for Game State**: Enable horizontal scaling
8. **WebSocket Updates**: Real-time game evolution
9. **Social Features**: Share saved games, multiplayer
10. **Analytics**: Track popular patterns, play time

### Long-Term (Roadmap)
11. **Mobile App**: React Native or ClojureScript mobile client
12. **Cloud Deployment**: Deploy to Heroku, AWS, or Fly.io
13. **Leaderboard Seasons**: Time-based competitive periods
14. **Custom Rules**: Allow users to modify Game of Life rules
15. **Pattern Library**: Curated collection of interesting patterns

---

## See Also

- [README.md](../README.md) - Quick start guide
- [API Documentation](./api.md) - REST API reference
- [Web Server README](../bases/web-server/README.md) - HTTP server details
- [Polylith Documentation](https://polylith.gitbook.io/) - Architecture guide
- [AGENTS.md](../AGENTS.md) - AI agent operational guidelines
