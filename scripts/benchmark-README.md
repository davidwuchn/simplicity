# Performance Benchmarking

This directory contains performance benchmarking scripts for Simplicity.

## Running Benchmarks

### All Benchmarks
```bash
clojure -M:dev -m simplicity.benchmark
```

### From REPL
```clojure
(require '[simplicity.benchmark :as bench])

;; Run all benchmarks
(bench/benchmark-all)

;; Run specific benchmarks
(bench/benchmark-game-evolution)
(bench/benchmark-user-operations)
(bench/benchmark-memory-usage)
```

## What Gets Benchmarked

### Game Component
- **Small board evolution** (10x10, 4 cells) - Baseline performance
- **Medium board evolution** (50x50, ~500 cells) - Typical gameplay
- **Large board evolution** (100x100, ~2000 cells) - Stress test
- **Pattern analysis** - Pattern recognition performance
- **Musical trigger generation** - Audio parameter generation
- **Score calculation** - Score computation

### User Component
- **User lookup** - Database query by username
- **High score retrieval** - Score query performance
- **High score update** - Atomic score update
- **Leaderboard generation** - Top 10 query with sorting
- **Password verification** - bcrypt hashing performance

### Memory Usage
- Total/free/used memory
- JVM heap statistics
- Memory usage percentage

## Sample Output

```
╔════════════════════════════════════════════════╗
║   Simplicity Performance Benchmarks           ║
╚════════════════════════════════════════════════╝

JVM Information:
  Java Version:     17.0.9
  JVM Name:         OpenJDK 64-Bit Server VM
  Available CPUs:   8

========== Memory Usage ==========
  Total memory:     256.00 MB
  Free memory:      180.23 MB
  Used memory:      75.77 MB
  Max memory:       4096.00 MB
  Memory usage:     1.8%

========== Game Component Benchmarks ==========

Small board evolution (10x10, 4 cells):
  Iterations: 1000
  Min:        125.00 μs
  Max:        2.45 ms
  Mean:       187.34 μs (5.34 K ops/sec)
  Median:     175.50 μs

Medium board evolution (50x50, ~500 cells):
  Iterations: 100
  Min:        3.12 ms
  Max:        8.76 ms
  Mean:       4.23 ms (236.41 ops/sec)
  Median:     4.15 ms

Large board evolution (100x100, ~2000 cells):
  Iterations: 50
  Min:        15.34 ms
  Max:        28.91 ms
  Mean:       18.67 ms (53.56 ops/sec)
  Median:     18.12 ms

Pattern analysis:
  Iterations: 100
  Min:        450.00 μs
  Max:        3.21 ms
  Mean:       625.78 μs (1.60 K ops/sec)
  Median:     598.23 μs

Musical trigger generation:
  Iterations: 100
  Min:        523.45 μs
  Max:        4.12 ms
  Mean:       712.34 μs (1.40 K ops/sec)
  Median:     687.90 μs

Score calculation:
  Iterations: 1000
  Min:        12.34 μs
  Max:        456.78 μs
  Mean:       34.56 μs (28.94 K ops/sec)
  Median:     28.90 μs

========== User Component Benchmarks ==========

User lookup by username:
  Iterations: 1000
  Min:        234.56 μs
  Max:        5.67 ms
  Mean:       387.23 μs (2.58 K ops/sec)
  Median:     345.67 μs

High score retrieval:
  Iterations: 1000
  Min:        198.34 μs
  Max:        3.45 ms
  Mean:       312.45 μs (3.20 K ops/sec)
  Median:     289.12 μs

High score update:
  Iterations: 100
  Min:        456.78 μs
  Max:        8.90 ms
  Mean:       723.45 μs (1.38 K ops/sec)
  Median:     689.23 μs

Leaderboard generation:
  Iterations: 100
  Min:        567.89 μs
  Max:        12.34 ms
  Mean:       892.34 μs (1.12 K ops/sec)
  Median:     834.56 μs

Password verification (bcrypt):
  Iterations: 10
  Min:        87.45 ms
  Max:        112.34 ms
  Mean:       95.67 ms (10.45 ops/sec)
  Median:     94.23 ms

========== Memory Usage ==========
  Total memory:     512.00 MB
  Free memory:      287.45 MB
  Used memory:      224.55 MB
  Max memory:       4096.00 MB
  Memory usage:     5.5%

╔════════════════════════════════════════════════╗
║   Benchmark Complete                          ║
╚════════════════════════════════════════════════╝
```

## Performance Targets

### Game Component
| Operation | Target | Acceptable |
|-----------|--------|------------|
| Small board evolution | <200 μs | <500 μs |
| Medium board evolution | <5 ms | <10 ms |
| Pattern analysis | <1 ms | <2 ms |
| Musical triggers | <1 ms | <2 ms |
| Score calculation | <50 μs | <100 μs |

### User Component
| Operation | Target | Acceptable |
|-----------|--------|------------|
| User lookup | <500 μs | <1 ms |
| High score retrieval | <500 μs | <1 ms |
| High score update | <1 ms | <2 ms |
| Leaderboard | <1 ms | <3 ms |
| Password verification | ~100 ms | ~150 ms |

**Note**: Password verification is intentionally slow (bcrypt security feature).

## Memory Targets

- **Idle usage**: <100 MB
- **Active game**: <500 MB
- **Peak usage**: <1 GB

## Interpreting Results

### Good Performance
- Mean close to median (consistent performance)
- Min/max within 2x of mean (low variance)
- Ops/sec meets or exceeds targets

### Performance Issues
- Large gap between min and max (high variance)
- Mean significantly higher than target
- Memory usage growing over time

### Common Causes of Slow Performance
1. **GC Pressure**: High memory usage triggers frequent garbage collection
2. **Database Locking**: SQLite locks causing contention
3. **Large Boards**: Too many living cells (>3000)
4. **JVM Warmup**: First few runs slower (use warmup iterations)

## Optimization Tips

### Game Evolution
```clojure
;; Use transients for large boards
(persistent! (reduce conj! (transient #{}) new-cells))

;; Cache neighbor calculations
(def neighbor-offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]])
```

### Database Queries
```clojure
;; Use prepared statements (already done)
;; Add indexes for frequently queried columns
CREATE INDEX idx_users_username ON users(username);

;; Use connection pooling for high concurrency
```

### Memory Management
```bash
# Increase heap size for large games
java -Xmx2g -Xms1g -jar simplicity-standalone.jar

# Use G1GC for better latency
java -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar simplicity-standalone.jar
```

## Continuous Monitoring

### Baseline Benchmarks
Run benchmarks after major changes and compare to baseline:

```bash
# Save baseline
clojure -M:dev -m simplicity.benchmark > benchmarks/baseline-v1.0.0.txt

# Compare after changes
clojure -M:dev -m simplicity.benchmark > benchmarks/current.txt
diff benchmarks/baseline-v1.0.0.txt benchmarks/current.txt
```

### Automated Benchmarking (CI)
Add to `.github/workflows/benchmark.yml`:
```yaml
name: Benchmark
on:
  push:
    branches: [master]
jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run benchmarks
        run: clojure -M:dev -m simplicity.benchmark
```

## Profiling

For deeper analysis, use JVM profiling tools:

### VisualVM
```bash
# Start application with JMX
java -Dcom.sun.management.jmxremote -jar simplicity-standalone.jar

# Connect VisualVM to running process
jvisualvm
```

### JProfiler / YourKit
Commercial profilers with advanced features:
- CPU profiling
- Memory allocation tracking
- Thread analysis
- Database query profiling

### Clojure-specific
```clojure
;; Use criterium for detailed benchmarking
(require '[criterium.core :as crit])

(crit/bench (game/evolve! :my-game))
;; => Detailed statistical analysis
```

## See Also

- [Architecture Documentation](../docs/architecture.md) - Performance characteristics
- [TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md) - Performance issues
- [Game Component](../components/game/README.md) - Game performance details
- [User Component](../components/user/README.md) - Database performance

---

**Run benchmarks**: `clojure -M:dev -m simplicity.benchmark`
