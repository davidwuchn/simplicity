(ns simplicity.benchmark
  "Performance benchmarking for Simplicity components.
   
   Run with: clojure -M:dev -m simplicity.benchmark"
  (:require [cc.mindward.game.interface :as game]
            [cc.mindward.user.interface :as user]
            [clojure.string :as str]))

(defn format-duration [nanos]
  "Format nanoseconds into human-readable duration."
  (cond
    (< nanos 1000) (str nanos " ns")
    (< nanos 1000000) (str (format "%.2f" (/ nanos 1000.0)) " μs")
    (< nanos 1000000000) (str (format "%.2f" (/ nanos 1000000.0)) " ms")
    :else (str (format "%.2f" (/ nanos 1000000000.0)) " s")))

(defn format-ops-per-sec [nanos]
  "Calculate operations per second from average nanoseconds."
  (let [ops-per-sec (/ 1000000000.0 nanos)]
    (cond
      (>= ops-per-sec 1000000) (str (format "%.2f" (/ ops-per-sec 1000000.0)) " M ops/sec")
      (>= ops-per-sec 1000) (str (format "%.2f" (/ ops-per-sec 1000.0)) " K ops/sec")
      :else (str (format "%.0f" ops-per-sec) " ops/sec"))))

(defmacro bench
  "Benchmark an expression. Returns map with :min, :max, :mean, :median."
  [iterations & body]
  `(let [warmup# 10
         iterations# ~iterations]
     ;; Warmup
     (dotimes [_# warmup#]
       ~@body)
     
     ;; Benchmark
     (let [times# (vec (for [_# (range iterations#)]
                         (let [start# (System/nanoTime)]
                           ~@body
                           (- (System/nanoTime) start#))))
           sorted# (sort times#)
           mean# (/ (reduce + sorted#) (count sorted#))
           median# (nth sorted# (quot (count sorted#) 2))]
       {:min (first sorted#)
        :max (last sorted#)
        :mean mean#
        :median median#
        :iterations iterations#})))

(defn print-benchmark [name result]
  "Print benchmark results in a formatted table."
  (println (str "\n" name ":"))
  (println (str "  Iterations: " (:iterations result)))
  (println (str "  Min:        " (format-duration (:min result))))
  (println (str "  Max:        " (format-duration (:max result))))
  (println (str "  Mean:       " (format-duration (:mean result)) " (" (format-ops-per-sec (:mean result)) ")"))
  (println (str "  Median:     " (format-duration (:median result)))))

(defn benchmark-game-evolution []
  "Benchmark game evolution performance."
  (println "\n========== Game Component Benchmarks ==========")
  
  ;; Initialize
  (game/initialize!)
  
  ;; Small board (10x10, sparse)
  (game/create-game! :bench-small #{[5 5] [5 6] [6 5] [6 6]})
  (let [result (bench 1000 (game/evolve! :bench-small))]
    (print-benchmark "Small board evolution (10x10, 4 cells)" result))
  
  ;; Medium board (50x50, random)
  (let [random-cells (set (for [_ (range 500)]
                             [(rand-int 50) (rand-int 50)]))]
    (game/create-game! :bench-medium random-cells)
    (let [result (bench 100 (game/evolve! :bench-medium))]
      (print-benchmark "Medium board evolution (50x50, ~500 cells)" result)))
  
  ;; Large board (100x100, random)
  (let [random-cells (set (for [_ (range 2000)]
                             [(rand-int 100) (rand-int 100)]))]
    (game/create-game! :bench-large random-cells)
    (let [result (bench 50 (game/evolve! :bench-large))]
      (print-benchmark "Large board evolution (100x100, ~2000 cells)" result)))
  
  ;; Pattern analysis
  (let [result (bench 100 (game/get-pattern-analysis :bench-medium))]
    (print-benchmark "Pattern analysis" result))
  
  ;; Musical triggers
  (let [result (bench 100 (game/get-musical-triggers :bench-medium))]
    (print-benchmark "Musical trigger generation" result))
  
  ;; Score calculation
  (let [result (bench 1000 (game/get-score :bench-medium))]
    (print-benchmark "Score calculation" result)))

(defn benchmark-user-operations []
  "Benchmark user component performance."
  (println "\n========== User Component Benchmarks ==========")
  
  ;; Initialize
  (user/init!)
  
  ;; Create test users
  (dotimes [i 10]
    (try
      (user/create-user! {:username (str "bench-user-" i)
                          :password "BenchmarkPass123"
                          :name (str "Benchmark User " i)})
      (catch Exception _)))
  
  ;; User lookup
  (let [result (bench 1000 (user/find-by-username "bench-user-5"))]
    (print-benchmark "User lookup by username" result))
  
  ;; High score retrieval
  (let [result (bench 1000 (user/get-high-score "bench-user-5"))]
    (print-benchmark "High score retrieval" result))
  
  ;; High score update
  (let [result (bench 100 (user/update-high-score! "bench-user-5" (rand-int 10000)))]
    (print-benchmark "High score update" result))
  
  ;; Leaderboard generation
  (let [result (bench 100 (user/get-leaderboard))]
    (print-benchmark "Leaderboard generation" result))
  
  ;; Password verification
  (let [user (user/find-by-username "bench-user-5")
        result (bench 10 (user/verify-password "BenchmarkPass123" (:password user)))]
    (print-benchmark "Password verification (bcrypt)" result)))

(defn benchmark-memory-usage []
  "Report memory usage statistics."
  (println "\n========== Memory Usage ==========")
  (let [runtime (Runtime/getRuntime)
        mb (/ 1024.0 1024.0)]
    (System/gc)
    (Thread/sleep 100)
    (let [total-memory (/ (.totalMemory runtime) mb)
          free-memory (/ (.freeMemory runtime) mb)
          used-memory (- total-memory free-memory)
          max-memory (/ (.maxMemory runtime) mb)]
      (println (format "  Total memory:     %.2f MB" total-memory))
      (println (format "  Free memory:      %.2f MB" free-memory))
      (println (format "  Used memory:      %.2f MB" used-memory))
      (println (format "  Max memory:       %.2f MB" max-memory))
      (println (format "  Memory usage:     %.1f%%" (* 100.0 (/ used-memory max-memory)))))))

(defn benchmark-all []
  "Run all benchmarks."
  (println "\n╔════════════════════════════════════════════════╗")
  (println "║   Simplicity Performance Benchmarks           ║")
  (println "╚════════════════════════════════════════════════╝")
  (println "\nJVM Information:")
  (println (str "  Java Version:     " (System/getProperty "java.version")))
  (println (str "  JVM Name:         " (System/getProperty "java.vm.name")))
  (println (str "  Available CPUs:   " (.availableProcessors (Runtime/getRuntime))))
  
  (benchmark-memory-usage)
  (benchmark-game-evolution)
  (benchmark-user-operations)
  (benchmark-memory-usage)
  
  (println "\n╔════════════════════════════════════════════════╗")
  (println "║   Benchmark Complete                          ║")
  (println "╚════════════════════════════════════════════════╝\n"))

(defn -main [& args]
  "Main entry point for benchmarking."
  (benchmark-all)
  (shutdown-agents))

;; Run benchmarks when file is evaluated directly
(comment
  ;; Run all benchmarks
  (benchmark-all)
  
  ;; Run specific benchmark
  (benchmark-game-evolution)
  (benchmark-user-operations)
  
  ;; Quick memory check
  (benchmark-memory-usage)
  )
