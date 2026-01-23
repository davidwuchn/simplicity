# ADR-002: SQLite for Persistence

**Date**: 2026-01-22  
**Status**: Accepted  
**Author**: Simplicity Team

## Context

**Problem Statement:**

Need a persistence layer for user data, high scores, and game state that is simple, reliable, and suitable for a small-to-medium scale application.

**Current Situation:**

Application requirements:
- Store user credentials and profiles
- Persist high scores (leaderboard)
- Simple schema (3-4 tables max)
- Expected scale: <10k users, <100k games
- Single-server deployment initially

**Constraints:**
- Must be embeddable (no separate database server for development)
- ACID compliance required (financial/score data)
- Must support concurrent reads/writes
- Low operational overhead
- Cost-effective for small deployments

**Assumptions:**
- Single-instance deployment (no distributed system initially)
- Read-heavy workload (leaderboard queries >> updates)
- Can scale vertically if needed
- May migrate to PostgreSQL if growth requires

## Decision

**Chosen Solution:**

Use **SQLite** as the primary database with the following configuration:

```clojure
;; Connection pool via HikariCP
{:jdbcUrl "jdbc:sqlite:./simplicity.db"
 :maximumPoolSize 10
 :connectionTimeout 3000}
```

**Features Used:**
- Write-Ahead Logging (WAL mode) for concurrent access
- Foreign key constraints enabled
- Parameterized queries via next.jdbc
- Connection pooling via HikariCP

**Rationale:**

1. **Zero Configuration**: No separate database server, embedded in application
2. **ACID Compliance**: Full transaction support, suitable for score tracking
3. **Performance**: Fast for read-heavy workloads (<100k records)
4. **Portability**: Single file database, easy backups and migrations
5. **Development Experience**: Same database in dev and production
6. **Battle-Tested**: Used by major applications (iOS apps, browsers, etc.)

**Alternatives Considered:**

1. **PostgreSQL**
   - Pros: Better concurrency, full-featured, industry standard
   - Cons: Requires separate server, operational overhead, overkill for current scale
   - Why rejected: Unnecessary complexity for initial deployment, can migrate later if needed

2. **In-Memory (Atoms/Agents)**
   - Pros: Simplest implementation, fastest
   - Cons: No persistence, data loss on restart, no ACID
   - Why rejected: Unacceptable data loss risk for user accounts and scores

3. **Datomic/DataScript**
   - Pros: Immutable facts, time travel, powerful queries
   - Cons: Learning curve, licensing (Datomic), limited deployment options
   - Why rejected: Over-engineered for current needs

4. **MongoDB/NoSQL**
   - Pros: Flexible schema, horizontal scaling
   - Cons: No ACID (older versions), schema management burden, heavier resource usage
   - Why rejected: Relational data model fits our domain well

## Consequences

**Positive:**
- **Simple Deployment**: No database server to manage, runs in-process
- **Easy Backups**: Copy single `.db` file
- **Fast Development**: No Docker/container setup needed
- **Low Resource Usage**: Minimal memory footprint (~few MB)
- **Reliable**: ACID transactions prevent data corruption
- **Migration Path**: Can export to PostgreSQL if needed (via `sqlite3 .dump`)

**Negative:**
- **Concurrency Limits**: Write serialization (WAL mode helps but doesn't eliminate)
- **No Network Access**: Can't connect from external tools easily
- **Scaling Ceiling**: Not suitable for >100k concurrent users or distributed deployments
- **Limited Tooling**: Fewer admin tools compared to PostgreSQL

**Neutral:**
- **File-Based**: Database is a file (can be pro or con depending on deployment)
- **Type System**: Dynamic typing (less strict than PostgreSQL)

**Risks:**
- **Write Contention**: High write volume could cause lock contention
- **File Corruption**: Database file corruption on unclean shutdown
- **Migration Complexity**: Moving to PostgreSQL later requires data migration

**Mitigation:**
- **WAL Mode**: Enabled for better concurrent write handling
- **Connection Pool**: Limit connections via HikariCP (max 10)
- **Backup Strategy**: Automated daily backups (see deployment scripts)
- **Monitoring**: Track database lock errors in logs
- **Migration Ready**: Use next.jdbc abstractions (not SQLite-specific SQL)

## Implementation

**Changes Required:**
1. ✅ Add SQLite JDBC driver to `deps.edn`
2. ✅ Implement datasource creation in `user/impl`
3. ✅ Enable WAL mode and foreign keys on connection
4. ✅ Create schema initialization (users, games tables)
5. ✅ Add connection pooling via HikariCP

**Schema:**
```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  name TEXT,
  high_score INTEGER DEFAULT 0,
  created_at INTEGER DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE games (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  board TEXT NOT NULL,  -- JSON serialized
  score INTEGER DEFAULT 0,
  generation INTEGER DEFAULT 0,
  created_at INTEGER DEFAULT (strftime('%s', 'now'))
);
```

**Testing Strategy:**
- ✅ Use temp databases in tests (`java.io.File/createTempFile`)
- ✅ Isolate tests with fixtures (`use-fixtures :each`)
- ✅ Test SQL injection prevention (36 assertions)
- ✅ Test concurrent access patterns

**Rollout Plan:**
- ✅ Phase 1: Implement in development environment
- ✅ Phase 2: Add automated backups (daily cron)
- ✅ Phase 3: Monitor performance in production
- ⏳ Phase 4: Evaluate PostgreSQL migration at 10k users

## Related Documents

- [SQLite Documentation](https://www.sqlite.org/docs.html)
- [SQLite WAL Mode](https://www.sqlite.org/wal.html)
- [ADR-004: Security-First Design](./004-security-first-design.md) (parameterized queries)
- [Deployment Guide](../deployment-cloudflare.md) (backup strategy)

## Notes

**Performance Characteristics:**
- Read latency: <1ms for indexed queries
- Write latency: ~5-10ms (WAL mode)
- Leaderboard query (top 100): <2ms
- Database size: ~1MB per 1000 users (rough estimate)

**Migration Path to PostgreSQL:**
If needed, migration involves:
1. Export schema and data: `sqlite3 simplicity.db .dump > export.sql`
2. Convert SQLite SQL to PostgreSQL dialect
3. Update datasource configuration
4. Re-run tests (next.jdbc abstraction should minimize changes)
5. Deploy with zero-downtime script

**Security Validated:**
- ✅ Parameterized queries (no string concatenation)
- ✅ SQL injection tests pass (36 assertions)
- ✅ Password hashing (bcrypt+sha512, never stored plain)
- ✅ Input validation at boundaries
