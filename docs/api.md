# API Documentation

## REST API Reference

### Base URL
```
http://localhost:3000
```

All API endpoints require an active session (cookie-based authentication).

---

## Authentication Endpoints

### POST /login
Authenticate a user and create a session.

**Request:**
```http
POST /login
Content-Type: application/x-www-form-urlencoded

username=alice&password=secret123
```

**Response (Success):**
```http
HTTP/1.1 302 Found
Location: /game
Set-Cookie: ring-session=...
```

**Response (Failure):**
```http
HTTP/1.1 302 Found
Location: /login?error=true
```

---

### POST /signup
Create a new user account.

**Request:**
```http
POST /signup
Content-Type: application/x-www-form-urlencoded

username=alice&password=secret123&name=Alice%20Wonder
```

**Parameters:**
- `username` (required): Unique username, 3-50 characters
- `password` (required): Minimum 6 characters
- `name` (required): Display name for leaderboard

**Response (Success):**
```http
HTTP/1.1 302 Found
Location: /game
Set-Cookie: ring-session=...
```

**Response (Failure):**
```http
HTTP/1.1 302 Found
Location: /signup?error=true
```

**Error Reasons:**
- Username already exists
- Missing required fields
- Database constraint violation

---

### GET /logout
End the current session.

**Response:**
```http
HTTP/1.1 302 Found
Location: /login
Set-Cookie: ring-session=deleted; Max-Age=0
```

---

## Page Endpoints

### GET /
Landing page. Redirects authenticated users to `/game`.

### GET /login
Login page.

### GET /signup
Signup page.

### GET /game
**Requires authentication.**

Main game interface with Game of Life canvas.

**Response:**
```html
HTTP/1.1 200 OK
Content-Type: text/html

<html>...</html>
```

### GET /leaderboard
Global leaderboard showing top scores.

**Response:**
```html
HTTP/1.1 200 OK
Content-Type: text/html

<html>
  <table>
    <tr><td>KING</td><td>Alice</td><td>1500</td></tr>
    ...
  </table>
</html>
```

---

## Game API Endpoints

### POST /api/game
**Requires authentication.**

Main game interaction endpoint. Behavior depends on `action` parameter.

**Common Headers:**
```http
POST /api/game
Content-Type: application/x-www-form-urlencoded
X-CSRF-Token: <token from hidden input>
```

#### Action: create
Initialize or reset the game board with specific cells.

**Request:**
```http
action=create&cells=[[0,0],[1,1],[2,2]]
```

**Parameters:**
- `action=create`
- `cells` (JSON array): Array of [x, y] coordinates

**Response:**
```json
{
  "board": [[0,0], [1,1], [2,2]],
  "generation": 0,
  "score": 3
}
```

**Example (curl):**
```bash
curl -X POST http://localhost:3000/api/game \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=create&cells=[[0,0],[1,0],[2,0]]'
```

---

#### Action: evolve
Advance the game one generation using Conway's rules.

**Request:**
```http
action=evolve
```

**Response:**
```json
{
  "board": [[1,-1], [1,0], [1,1]],
  "generation": 1,
  "score": 5,
  "triggers": [
    {"trigger": "density-mid", "params": {"frequency": 220, "amplitude": 0.6}},
    {"trigger": "life-pulse", "params": {"rate": 0.333, "intensity": 0.03}},
    {"trigger": "drone", "params": {"frequency": 55, "amplitude": 0.3}}
  ]
}
```

**Fields:**
- `board`: Updated cell positions after evolution
- `generation`: Generation count (increments by 1)
- `score`: Game score based on complexity and longevity
- `triggers`: Musical trigger events for synthesizer

**Musical Triggers:**
| Trigger | Condition | Purpose |
|---------|-----------|---------|
| `density-high` | >50 cells alive | Chaotic high-energy sounds |
| `density-mid` | >20 cells alive | Mid-range rhythmic elements |
| `life-pulse` | Any cells alive | Pulse rate based on cell count |
| `drone` | Always present | Sustained bass tone |

**Example (curl):**
```bash
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=evolve'
```

---

#### Action: manipulate
Add or remove cells from the board without evolving.

**Request:**
```http
action=manipulate&cells=[[5,5],[6,6]]&remove=[[0,0]]
```

**Parameters:**
- `action=manipulate`
- `cells` (JSON array): Cells to add
- `remove` (JSON array): Cells to remove

**Response:**
```json
{
  "board": [[5,5], [6,6], [1,1], [2,2]],
  "generation": 1,
  "score": 7
}
```

**Example (curl):**
```bash
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=manipulate&cells=[[10,10]]&remove=[[0,0]]'
```

---

#### Action: save
Save the current game state for later.

**Request:**
```http
action=save&name=My%20Awesome%20Pattern
```

**Parameters:**
- `action=save`
- `name` (required): Human-readable name for the saved game

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Awesome Pattern",
  "saved": true
}
```

**Fields:**
- `id`: UUID for loading later
- `name`: Saved game name
- `saved`: Boolean confirmation

**Example (curl):**
```bash
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=save&name=Glider%20Gun'
```

---

#### Action: load
Load a previously saved game.

**Request:**
```http
action=load&savedId=550e8400-e29b-41d4-a716-446655440000
```

**Parameters:**
- `action=load`
- `savedId` (required): UUID from save response

**Response:**
```json
{
  "board": [[0,1], [1,2], [2,0], [2,1], [2,2]],
  "generation": 0,
  "score": 5,
  "loaded": true
}
```

**Note:** Loaded games reset to generation 0 but preserve the board state.

**Example (curl):**
```bash
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=load&savedId=550e8400-e29b-41d4-a716-446655440000'
```

---

### GET /api/games
**Requires authentication.**

List all saved games.

**Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Glider Gun",
    "generation": 5,
    "score": 150
  },
  {
    "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "name": "Spaceship",
    "generation": 3,
    "score": 75
  }
]
```

**Example (curl):**
```bash
curl http://localhost:3000/api/games \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt
```

---

### POST /game/score
**Requires authentication.**

Update the user's high score.

**Request:**
```http
POST /game/score
Content-Type: application/x-www-form-urlencoded

score=1500
```

**Parameters:**
- `score` (integer): New score value

**Response:**
```json
{
  "highScore": 1500
}
```

**Note:** Only updates if the new score is higher than the current high score.

**Example (curl):**
```bash
curl -X POST http://localhost:3000/game/score \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'score=1500'
```

---

## Error Responses

### Invalid Action
```json
{
  "error": "Invalid action"
}
```

### Missing Parameters
The API fails gracefully, returning `nil` for missing required parameters (e.g., save without name).

### Unauthenticated Requests
Game endpoints return `nil` or redirect to `/login` when accessed without a valid session.

---

## CSRF Protection

All `POST` requests require a valid CSRF token.

**Getting the Token:**
1. Extract from hidden input on any page:
   ```html
   <input type="hidden" name="__anti-forgery-token" value="..." />
   ```

2. Include in requests:
   ```http
   X-CSRF-Token: <token value>
   ```

**Example:**
```bash
# Extract token from page
TOKEN=$(curl -s -b cookies.txt http://localhost:3000/game | \
        grep -oP '(?<=__anti-forgery-token" value=")[^"]+')

# Use in API call
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=evolve'
```

---

## Rate Limiting

Currently, no rate limiting is implemented.

---

## Session Management

Sessions are cookie-based using Ring's session middleware.

**Session Cookie:**
- Name: `ring-session`
- HttpOnly: Yes
- Secure: No (development mode)
- SameSite: Lax

**Session Expiry:**
- Default: Browser session (cookie deleted on browser close)
- No server-side expiry configured

**Session Data:**
```clojure
{:username "alice"}
```

---

## Examples

### Complete Authentication Flow (Bash)

```bash
#!/bin/bash

# 1. Signup
curl -X POST http://localhost:3000/signup \
  -c cookies.txt \
  -L \
  -d 'username=testuser&password=test123&name=Test%20User'

# 2. Get CSRF token
TOKEN=$(curl -s -b cookies.txt http://localhost:3000/game | \
        grep -oP '(?<=__anti-forgery-token" value=")[^"]+')

# 3. Create game
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=create&cells=[[0,0],[1,0],[2,0]]' | jq .

# 4. Evolve
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=evolve' | jq .

# 5. Save game
curl -X POST http://localhost:3000/api/game \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'action=save&name=My%20Pattern' | jq .

# 6. List saved games
curl http://localhost:3000/api/games \
  -b cookies.txt | jq .

# 7. Update score
curl -X POST http://localhost:3000/game/score \
  -H "X-CSRF-Token: $TOKEN" \
  -b cookies.txt \
  -d 'score=1000' | jq .

# 8. Logout
curl http://localhost:3000/logout \
  -b cookies.txt \
  -c cookies.txt \
  -L
```

---

## Conway's Game of Life Rules

The game engine follows classic Conway's Game of Life rules:

1. **Survival**: Live cell with 2-3 neighbors survives
2. **Birth**: Dead cell with exactly 3 neighbors becomes alive
3. **Death**: All other cells die

**Bounds:**
- Min: `[-100, -100]`
- Max: `[100, 100]`
- Cells outside bounds are filtered out

**Performance:**
- Board represented as a `Set` of `[x y]` coordinates
- Evolution uses transient collections for efficiency
- Pattern recognition runs in O(n) where n = board size

---

## See Also

- [Architecture Documentation](./architecture.md)
- [Web Server README](../bases/web-server/README.md)
- [Game Component Documentation](../components/game/README.md)
