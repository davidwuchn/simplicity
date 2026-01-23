# Security Documentation

## Overview

This document outlines the security controls implemented in the Simplicity application. The application follows defense-in-depth principles with multiple layers of protection.

## Security Controls Implemented

### 1. Authentication & Authorization

#### Password Security
- **Hashing Algorithm**: bcrypt+sha512 (buddy-hashers)
- **Work Factor**: Elevated bcrypt cost (2^12 = 4096 iterations)
  - **Why 12?**: Provides ~4x stronger resistance to GPU cracking than default cost 10
  - Computation time: ~200-400ms per hash (acceptable for login)
- **Timing Attack Prevention**: Constant-time password verification via buddy-hashers
- **No Plain Text Storage**: Passwords are hashed before database storage

#### Session Management
- **Session Storage**: Ring session middleware (cookie-based)
- **CSRF Protection**: Anti-forgery tokens on all state-changing operations
- **Session Fixation Protection**: New session on login
- **Session Isolation**: Sessions are user-specific and validated on every request

### 2. Input Validation

#### Username Validation (`security/validate-username`)
- **Length**: 3-32 characters
- **Allowed Characters**: Alphanumeric, dash, underscore only (`[a-zA-Z0-9_-]`)
- **Whitespace**: No leading/trailing whitespace allowed
- **Purpose**: Prevents injection attacks and ensures consistent data format

#### Password Validation (`security/validate-password`)
- **Minimum Length**: 8 characters
- **Purpose**: Enforces minimum password strength

#### Score Validation (`security/validate-score`)
- **Type**: Non-negative integer only
- **Range**: 0 to 1,000,000
- **Purpose**: Prevents integer overflow and invalid score manipulation

### 3. Rate Limiting

#### Implementation
- **Algorithm**: Token Bucket
- **Storage**: In-memory (per-process)
- **Granularity**: Per-IP address
- **Protected Endpoints**:
  - `/login`, `/signup`: Auth endpoints (10 burst, 0.5 refill)
  - `/api/game`, `/game/score`: Game endpoints (30 burst, 2.0 refill)

#### Configuration
| Endpoint | Max Burst | Refill Rate | Purpose |
|----------|-----------|-------------|---------|
| `/login`, `/signup` | 10 | 0.5/s (1 per 2s) | Prevent brute force |
| `/api/game`, `/game/score` | 30 | 2.0/s (15 per s) | Prevent API spam/DoS |

- **Response**: 429 Too Many Requests (with Retry-After header)

#### Production Considerations
For multi-server deployments, replace in-memory storage with Redis:
```clojure
;; Example Redis integration (not implemented)
(require '[carmine :as car])
(def rate-limit-store 
  (reify 
    IAtom
    (swap [_ f] (car/wcar redis-conn (f (car/get "rate-limits"))))))
```

### 4. Security Headers

All HTTP responses include the following security headers:

#### Content-Security-Policy (CSP)
```
default-src 'self';
script-src 'self' 'unsafe-inline';
style-src 'self' 'unsafe-inline';
img-src 'self' data:;
font-src 'self';
connect-src 'self';
frame-ancestors 'none';
```

**Purpose**: Prevents XSS attacks, code injection, and clickjacking

**Note**: `unsafe-inline` is used for inline game scripts. In production, consider using nonces or hashes.

#### X-Frame-Options
```
DENY
```
**Purpose**: Prevents the page from being embedded in iframes (clickjacking protection)

#### X-Content-Type-Options
```
nosniff
```
**Purpose**: Prevents MIME sniffing attacks

#### X-XSS-Protection
```
1; mode=block
```
**Purpose**: Enables browser's XSS filter (legacy, but harmless)

#### Referrer-Policy
```
strict-origin-when-cross-origin
```
**Purpose**: Limits referrer information leakage

#### Permissions-Policy
```
geolocation=(), microphone=(), camera=()
```
**Purpose**: Disables unnecessary browser features

#### Strict-Transport-Security (HSTS) - Optional
```
max-age=31536000; includeSubDomains
```
**Configuration**: Set `ENABLE_HSTS=true` environment variable

**Purpose**: Forces HTTPS connections

**Warning**: Only enable HSTS after verifying HTTPS works correctly. Once enabled, browsers will refuse non-HTTPS connections for 1 year.

### 5. SQL Injection Prevention

#### Parameterized Queries
All database queries use parameterized statements via `next.jdbc`:

```clojure
;; Safe (parameterized)
(jdbc/execute! ds ["SELECT * FROM users WHERE username = ?" username])

;; NEVER do this (vulnerable to SQL injection)
(jdbc/execute! ds [(str "SELECT * FROM users WHERE username = '" username "'")])
```

#### Test Coverage
Comprehensive SQL injection tests in `user/security-test`:
- Username injection attempts
- Password injection attempts
- Score update injection attempts
- Unicode and special character handling

### 6. Cross-Site Scripting (XSS) Prevention

#### Output Encoding
- **Hiccup Templates**: Automatic HTML entity escaping
- **User-Generated Content**: All dynamic content is escaped by default
- **Explicit Escaping**: `helpers/escape-html` function for defense-in-depth
  - Escapes: `<`, `>`, `"`, `'`, `&`
  - Used for: display names, game names, any user-controlled data

#### Defense-in-Depth
- **Leaderboard**: Display names explicitly escaped via `helpers/escape-html`
- **Game Names**: Validated and escaped in API responses
- **JSON Responses**: Proper Content-Type headers

#### Test Coverage
- XSS payload escaping in usernames
- Error message escaping
- Leaderboard display escaping

### 7. Cross-Site Request Forgery (CSRF) Prevention

#### Implementation
- **Middleware**: `ring.middleware.anti-forgery`
- **Token Storage**: Session-based
- **Protected Methods**: POST, PUT, DELETE
- **Token Validation**: Automatic via ring-defaults

#### Client-Side
All AJAX requests must include the CSRF token:
```javascript
fetch('/api/game', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'x-csrf-token': getCsrfToken()
  },
  body: JSON.stringify(data)
})
```

### 8. Secure Error Handling

#### Production Logging
- **Framework**: Logback (SLF4J)
- **Sensitive Data**: Never log passwords or tokens
- **Error Details**: Generic error pages for users, detailed logs for operators

#### Configuration
```bash
# Set log level (DEBUG, INFO, WARN, ERROR)
export LOG_LEVEL=INFO

# Set log file path
export LOG_PATH=./logs
```

### 9. Database Security

#### Configuration
- **Type**: SQLite (single-file database)
- **Location**: Configurable via `DB_PATH` environment variable
- **Permissions**: Ensure file permissions restrict access (600 or 640)
- **Constraints**: UNIQUE constraint on username prevents duplicates

#### Production Recommendations
For production deployments:
1. Use PostgreSQL or similar RDBMS
2. Enable TLS/SSL for database connections
3. Use separate database credentials per environment
4. Enable database audit logging

## Security Testing

### Test Coverage
Total: **501 passing assertions** across 7 test suites

#### User Component Security Tests (36 assertions)
- SQL injection prevention (username, password, score)
- Unicode handling
- Long input handling
- Password hashing verification
- Timing attack resistance
- Duplicate username prevention

#### Web Server Security Tests (65 assertions)
- Security headers verification
- Rate limiting logic
- Rate limit token refill
- Per-IP isolation
- Input validation (username, password, score)
- CSRF token requirement
- Session isolation
- Session authentication
- XSS escaping

### Running Security Tests
```bash
# Run all tests
clojure -M:poly test :dev

# Run only security tests
clojure -M:poly test brick:user
clojure -M:poly test brick:web-server
```

## Deployment Checklist

### Pre-Production
- [ ] Review and test all security controls
- [ ] Ensure HTTPS is configured
- [ ] Set strong database credentials
- [ ] Configure firewall rules
- [ ] Enable production logging
- [ ] Set appropriate file permissions

### Production Configuration
```bash
# Required
export DB_PATH=/var/lib/simplicity/production.db
export PORT=3000

# Recommended
export LOG_LEVEL=WARN
export LOG_PATH=/var/log/simplicity

# Optional (only after HTTPS is verified)
export ENABLE_HSTS=true

# For multi-server deployments
# TODO: Implement Redis-based rate limiting
# export REDIS_URL=redis://localhost:6379
```

### Monitoring
- Monitor authentication failure rates (potential brute force)
- Monitor rate limit 429 responses
- Alert on repeated CSRF token failures
- Track database query performance
- Monitor for SQL error patterns

## Known Limitations & Future Improvements

### Current Limitations
1. **Rate Limiting**: In-memory (per-process), not suitable for multi-server (requires Redis)
2. **CSP**: Uses `unsafe-inline` for scripts (convenience over security, needs nonces)
3. **Session Storage**: Cookie-based (limited scalability)
4. **Database**: SQLite (not suitable for high-concurrency production)
5. **Session Invalidation**: Logout clears session cookie but doesn't invalidate server-side

### Security Improvements Implemented (2024)
- ✅ **Elevated bcrypt work factor**: Increased from cost 10 to cost 12
- ✅ **Game API authorization**: Added authentication check and game ownership validation
- ✅ **Rate limiting on game endpoints**: Added 30 burst / 2.0s refill for `/api/game` and `/game/score`
- ✅ **Coordinate validation**: Added bounds checking and array size limits
- ✅ **XSS prevention**: Added explicit `escape-html` helper for defense-in-depth

### Planned Improvements
1. **Redis Rate Limiting**: Distributed rate limiting for multi-server deployments
2. **CSP Nonces**: Remove `unsafe-inline` by using nonce-based CSP
3. **Database Migration**: PostgreSQL with connection pooling
4. **Session Invalidation**: Proper server-side session invalidation on logout
5. **Security Scanning**: Automated dependency vulnerability scanning
6. **Penetration Testing**: Third-party security audit

## Incident Response

### Security Incident Procedure
1. **Detection**: Monitor logs for suspicious activity
2. **Containment**: Disable affected accounts/IP addresses
3. **Investigation**: Review logs, analyze attack pattern
4. **Remediation**: Patch vulnerabilities, update credentials
5. **Communication**: Notify affected users if data was compromised

### Log Analysis
```bash
# Find failed login attempts
grep "Failed login attempt" logs/simplicity.log

# Find rate limit violations
grep "Rate limit exceeded" logs/simplicity.log

# Find CSRF failures
grep "CSRF" logs/simplicity.log
```

## Security Contact

For security issues, please report to the repository maintainers via GitHub Issues (for public, non-critical issues) or private communication for critical vulnerabilities.

---

*Last Updated: 2024-01-15*
*Version: 1.1.0* - Security hardening updates:
- Elevated bcrypt work factor to cost 12
- Added game API authorization checks
- Added rate limiting to game endpoints
- Added coordinate validation with bounds checking
- Added explicit HTML escaping helper
