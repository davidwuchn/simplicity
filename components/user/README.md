# User Component

The User component manages user accounts, authentication data, and high scores. It provides database persistence with SQLite and secure password handling.

## Architecture

**Interface**: `cc.mindward.user.interface`  
**Implementation**: `cc.mindward.user.impl`

This component follows Polylith encapsulation - all business logic is accessed through the `interface` namespace.

## Features

- User account creation and lookup
- Secure password hashing (bcrypt + sha512)
- High score tracking with atomic updates
- Leaderboard generation
- SQLite database persistence
- Connection pooling (next.jdbc)
- SQL injection prevention (parameterized queries)

## Database Schema

### `users` Table
```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL,  -- bcrypt + sha512 hash
  name TEXT NOT NULL,
  high_score INTEGER DEFAULT 0,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_high_score ON users(high_score DESC);
```

## API Reference

### Initialization

#### `init!`
Initialize the user subsystem. Call once at application startup.

**Idempotent**: Safe to call multiple times.

```clojure
(require '[cc.mindward.user.interface :as user])

(user/init!)
```

### Query Operations (Pure Functions)

#### `find-by-username`
Find a user by username.

**Parameters**: `username` (string)  
**Returns**: User map or nil

```clojure
(user/find-by-username "alice")
;; => {:id 1
;;     :username "alice"
;;     :password "$2a$10$..." ;; bcrypt hash
;;     :name "Alice Smith"
;;     :high_score 5000
;;     :created_at "2026-01-22T10:30:00Z"}

(user/find-by-username "nonexistent")
;; => nil
```

#### `get-high-score`
Get the high score for a username.

**Parameters**: `username` (string)  
**Returns**: Integer or nil

```clojure
(user/get-high-score "alice")
;; => 5000

(user/get-high-score "nobody")
;; => nil
```

#### `get-leaderboard`
Get the top 10 users by high score.

**Returns**: Vector of maps with `:username`, `:name`, `:high_score`

```clojure
(user/get-leaderboard)
;; => [{:username "alice"
;;      :name "Alice Smith"
;;      :high_score 10000}
;;     {:username "bob"
;;      :name "Bob Jones"
;;      :high_score 8500}
;;     {:username "charlie"
;;      :name "Charlie Brown"
;;      :high_score 7200}
;;     ...]
```

### Command Operations (Side Effects)

#### `create-user!`
Create a new user account.

**Parameters**: Map with `:username`, `:password`, `:name`  
**Returns**: Created user map  
**Throws**: `SQLException` on duplicate username

```clojure
(user/create-user! {:username "alice"
                    :password "SecurePass123"
                    :name "Alice Smith"})
;; => {:id 1
;;     :username "alice"
;;     :password "$2a$10$..." ;; hashed
;;     :name "Alice Smith"
;;     :high_score 0
;;     :created_at "2026-01-22T10:30:00Z"}

;; Duplicate username throws
(user/create-user! {:username "alice"
                    :password "different"
                    :name "Another Alice"})
;; => SQLException: UNIQUE constraint failed: users.username
```

#### `update-high-score!`
Update user's high score if the new score is higher.

Uses SQL `MAX()` for atomic comparison (prevents race conditions).

**Parameters**:
- `username` (string)
- `score` (integer)

**Returns**: Number of rows updated (0 or 1)

```clojure
;; User currently has high score of 1000
(user/update-high-score! "alice" 1500)
;; => 1 (updated)

;; Lower score doesn't update
(user/update-high-score! "alice" 1200)
;; => 0 (not updated, 1500 is still higher)
```

### Authentication Support

#### `verify-password`
Verify a plain-text password against a stored hash.

**Parameters**:
- `password` (string) - Plain-text password
- `password-hash` (string) - bcrypt + sha512 hash from database

**Returns**: Boolean (true if valid, false otherwise)

```clojure
(let [user (user/find-by-username "alice")]
  (user/verify-password "SecurePass123" (:password user)))
;; => true

(let [user (user/find-by-username "alice")]
  (user/verify-password "WrongPassword" (:password user)))
;; => false
```

## Usage Examples

### User Registration Flow

```clojure
(require '[cc.mindward.user.interface :as user])

;; Initialize database
(user/init!)

;; Create new user
(try
  (let [new-user (user/create-user! {:username "alice"
                                      :password "SecurePass123"
                                      :name "Alice Smith"})]
    (println "User created:" (:username new-user)))
  (catch Exception e
    (println "Registration failed:" (.getMessage e))))
```

### Login Flow

```clojure
(defn login [username password]
  (if-let [user (user/find-by-username username)]
    (if (user/verify-password password (:password user))
      {:success true
       :user (dissoc user :password)} ;; Remove password from response
      {:success false
       :error "Invalid password"})
    {:success false
     :error "User not found"}))

(login "alice" "SecurePass123")
;; => {:success true
;;     :user {:id 1 :username "alice" :name "Alice Smith" :high_score 0}}

(login "alice" "WrongPass")
;; => {:success false :error "Invalid password"}
```

### High Score Update

```clojure
(defn save-game-score [username new-score]
  (let [updated (user/update-high-score! username new-score)]
    (if (pos? updated)
      (println "New high score!" new-score)
      (println "Score not high enough"))))

(save-game-score "alice" 5000)  ;; First score
;; => "New high score! 5000"

(save-game-score "alice" 3000)  ;; Lower than 5000
;; => "Score not high enough"

(save-game-score "alice" 7500)  ;; New high score
;; => "New high score! 7500"
```

### Leaderboard Display

```clojure
(defn show-leaderboard []
  (println "=== LEADERBOARD ===")
  (doseq [[idx entry] (map-indexed vector (user/get-leaderboard))]
    (printf "%2d. %-20s %10d (%s)\n"
            (inc idx)
            (:username entry)
            (:high_score entry)
            (:name entry))))

(show-leaderboard)
;; === LEADERBOARD ===
;;  1. alice               10000 (Alice Smith)
;;  2. bob                  8500 (Bob Jones)
;;  3. charlie              7200 (Charlie Brown)
;;  ...
```

## Password Security

### Hashing Algorithm
Passwords are hashed using **bcrypt** with **sha512** pre-hashing:
1. SHA512 hash the plain-text password (prevents bcrypt 72-byte limit issues)
2. bcrypt the SHA512 hash with work factor 10 (2^10 = 1024 rounds)

This provides:
- **Slow hashing**: ~100ms per password, resistant to brute force
- **Salt**: Unique salt per password (built into bcrypt)
- **Future-proof**: Can increase work factor over time

### Timing Attack Resistance
Password verification uses constant-time comparison (built into bcrypt) to prevent timing attacks.

### Security Testing
The user component has **49 passing assertions** covering:
- Password hashing and verification
- SQL injection prevention (36 assertions)
- Input validation
- Concurrent high score updates

See [docs/security.md](../../docs/security.md) for complete security details.

## Database Operations

### Connection Management
Uses `next.jdbc` with:
- Unqualified maps (`:username` instead of `:users/username`)
- Lowercase keys (`:high_score` not `:HIGH_SCORE`)
- Prepared statements for all queries (SQL injection prevention)

### Atomic Operations
High score updates use SQL `MAX()` function:
```sql
UPDATE users 
SET high_score = MAX(high_score, ?) 
WHERE username = ?
```

This is atomic at the database level - no race conditions even with concurrent requests.

### Indexing
Two indexes for performance:
- `idx_users_username` - Fast user lookup by username
- `idx_users_high_score` - Fast leaderboard queries (descending order)

## Error Handling

### Duplicate Username
```clojure
(try
  (user/create-user! {:username "alice" :password "pass" :name "Alice"})
  (catch java.sql.SQLException e
    (when (re-find #"UNIQUE constraint" (.getMessage e))
      (println "Username already taken"))))
```

### User Not Found
```clojure
(if-let [user (user/find-by-username "alice")]
  (println "Found user:" (:name user))
  (println "User not found"))
```

### Invalid Password
```clojure
(let [user (user/find-by-username "alice")]
  (if (user/verify-password password (:password user))
    (println "Login successful")
    (println "Invalid password")))
```

## Performance Characteristics

- **User Lookup**: O(1) - indexed by username
- **High Score Update**: O(1) - single UPDATE with WHERE clause
- **Leaderboard**: O(n log n) - sorted by high_score index, limited to 10 rows
- **Password Hashing**: ~100ms per password (intentionally slow for security)
- **Password Verification**: ~100ms (same as hashing)

## Testing

```bash
# Run user component tests
clojure -M:poly test brick:user

# Expected: 49 assertions, all passing
```

Tests cover:
- User creation and retrieval
- Password hashing and verification
- High score updates (including concurrency)
- Leaderboard generation
- SQL injection prevention
- Input validation
- Error cases (duplicate users, not found, etc.)

## Configuration

### Database Path
Set via environment variable `DB_PATH`:
```bash
export DB_PATH=/var/lib/simplicity/simplicity.db
```

Default: `./simplicity.db` (current directory)

### Database Tuning
SQLite configuration (set in implementation):
```clojure
;; WAL mode for better concurrency
PRAGMA journal_mode=WAL

;; Balanced durability vs. performance
PRAGMA synchronous=NORMAL

;; Memory cache
PRAGMA cache_size=-64000  ;; 64MB
```

## Migration Notes

### Schema Changes
If you need to modify the schema:
1. Backup database: `sqlite3 simplicity.db ".backup backup.db"`
2. Run ALTER TABLE or CREATE INDEX statements
3. Test thoroughly before production deployment

### Data Import
Import users from CSV:
```bash
sqlite3 simplicity.db <<EOF
.mode csv
.import users.csv users_temp
INSERT INTO users (username, password, name, high_score)
SELECT username, password, name, high_score FROM users_temp;
DROP TABLE users_temp;
EOF
```

## See Also

- [Auth Component](../auth/README.md) - Authentication logic
- [Game Component](../game/README.md) - Score generation
- [Web Server](../../bases/web-server/README.md) - HTTP endpoints
- [Security Documentation](../../docs/security.md) - Security controls

---

**Location**: `components/user/src/cc/mindward/user/`  
**Tests**: `components/user/test/cc/mindward/user/`  
**Lines**: ~300 (interface + implementation)  
**Database**: SQLite 3.x
