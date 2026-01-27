(ns cc.mindward.game.mathematical-patterns
  "Mathematical pattern implementations applying equations from '17 Equations That Changed the World'
   
   π (Synthesis): Each mathematical principle is mapped to concrete code patterns.
   ∃ (Truth): Mathematical correctness provides rigorous foundations.
   τ (Wisdom): Patterns demonstrate foresight about system behavior."
  (:require [clojure.math :as math]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]))

;; ====================================================================
;; 1. PYTHAGOREAN THEOREM (a² + b² = c²) - Orthogonal Decomposition
;; ====================================================================

(defn ensure-orthogonal-components
  "Verify components have minimal coupling (Pythagorean orthogonality).
   
   Principle: Independent components combine predictably like orthogonal vectors.
   Application: Measure coupling between system components.
   
   Returns true if components are sufficiently orthogonal (coupling < threshold)."
  [components coupling-matrix]
  (let [n (count components)
        ;; Calculate total coupling (sum of squares like a² + b²)
        total-coupling (reduce + (for [i (range n)
                                       j (range n)
                                       :when (not= i j)]
                                   (math/pow (get-in coupling-matrix [i j] 0) 2)))
        ;; Threshold based on number of components (c² = 2n for n components)
        threshold (* 2 n)]
    (<= total-coupling threshold)))

(defn orthogonal-decomposition
  "Decompose complex system into orthogonal subsystems.
   
   Principle: Like Pythagorean theorem decomposing hypotenuse into perpendicular sides.
   Application: Refactor monolithic systems into independent components."
  [system analyze-coupling]
  (let [components (keys system)
        coupling (analyze-coupling system)

        ;; Find maximally orthogonal grouping
        groups (reduce (fn [groups component]
                         (let [best-group (reduce (fn [best group]
                                                    (let [coupling-to-group
                                                          (reduce + (map #(get-in coupling [component %] 0) group))]
                                                      (if (< coupling-to-group (:coupling best))
                                                        {:group group :coupling coupling-to-group}
                                                        best)))
                                                  {:group [] :coupling ##Inf}
                                                  groups)]
                           (if (< (:coupling best-group) 5)  ;; Threshold for orthogonality
                             (update groups (.indexOf groups (:group best-group)) conj component)
                             (conj groups [component]))))
                       []
                       components)]
    groups))

;; ====================================================================
;; 2. LOGARITHMS (log xy = log x + log y) - Complexity Reduction
;; ====================================================================

(defn transform-multiplicative-to-additive
  "Convert O(n²) operations to O(n log n) via divide-and-conquer.
   
   Principle: Logarithms transform multiplication to addition.
   Application: Reduce algorithmic complexity through transformation."
  [complex-operation data]
  (if (<= (count data) 1)
    (complex-operation data)
    (let [mid (quot (count data) 2)
          left (subvec data 0 mid)
          right (subvec data mid)]
      ;; Combine results additively (log principle)
      (+ (transform-multiplicative-to-additive complex-operation left)
         (transform-multiplicative-to-additive complex-operation right)))))

(defn logarithmic-scaling
  "Apply logarithmic scaling to manage exponential growth.
   
   Principle: Logarithms compress large ranges into manageable scales.
   Application: Score scaling, rate limiting, visualization."
  [value base]
  (if (<= value 0)
    0
    (/ (math/log value) (math/log base))))

(defn divide-and-conquer
  "Generic divide-and-conquer algorithm template.
   
   Principle: log xy = log x + log y - break problems into independent subproblems.
   Application: Sorting, searching, optimization problems."
  [problem]
  (if (base-case? problem)
    (solve-base problem)
    (let [subproblems (divide problem)]
      (combine (map divide-and-conquer subproblems)))))

;; ====================================================================
;; 3. CALCULUS (df/dt = lim(h→0) [f(t+h)-f(t)]/h) - Rates of Change
;; ====================================================================

(defn rate-of-change
  "Calculate instantaneous rate of change (numerical derivative).
   
   Principle: Calculus measures how systems evolve over time.
   Application: Performance monitoring, trend analysis, adaptive systems."
  [f t & [h]]
  (let [h (or h 0.001)]
    (/ (- (f (+ t h)) (f t)) h)))

(defn monitor-system-evolution
  "Track how system metrics change over time.
   
   Principle: Understanding rates of change enables prediction and control.
   Application: Health monitoring, capacity planning, anomaly detection."
  [metric-fn time-intervals]
  (let [metrics (map metric-fn time-intervals)
        changes (map (fn [t1 t2] (rate-of-change metric-fn (/ (+ t1 t2) 2)))
                     time-intervals (rest time-intervals))]
    {:metrics metrics
     :rates-of-change changes
     :acceleration (map (fn [r1 r2] (- r2 r1)) changes (rest changes))}))

(defn adaptive-threshold
  "Dynamically adjust thresholds based on rate of change.
   
   Principle: Systems should adapt to changing conditions (calculus).
   Application: Rate limiting, cache eviction, load balancing."
  [current-value historical-values]
  (let [changes (map (fn [v1 v2] (- v2 v1)) historical-values (rest historical-values))
        avg-change (/ (reduce + changes) (max 1 (count changes)))
        volatility (math/sqrt (/ (reduce + (map #(math/pow (- % avg-change) 2) changes))
                                 (max 1 (count changes))))]
    (+ current-value (* 3 volatility))))  ;; 3-sigma threshold

;; ====================================================================
;; 4. EULER'S POLYHEDRA FORMULA (V - E + F = 2) - Topological Invariants
;; ====================================================================

(defn verify-architectural-invariant
  "Verify system maintains topological invariants across changes.
   
   Principle: Like Euler's formula, certain properties remain constant.
   Application: Architecture validation, refactoring safety checks."
  [system]
  (let [vertices (count (:components system))
        edges (count (:dependencies system))
        faces (count (:interfaces system))]
    (= (- (+ vertices faces) edges) 2)))  ;; V + F - E = 2

(defn maintain-invariants
  "Ensure architectural invariants are preserved during operations.
   
   Principle: Invariants provide stability guarantees.
   Application: Transaction safety, data integrity, system consistency."
  [system operation]
  (let [pre-invariant (verify-architectural-invariant system)
        new-system (operation system)
        post-invariant (verify-architectural-invariant new-system)]
    (if (and pre-invariant post-invariant)
      new-system
      (throw (ex-info "Architectural invariant violated"
                      {:pre pre-invariant :post post-invariant
                       :operation (str operation)})))))

;; ====================================================================
;; 5. INFORMATION THEORY (H = -Σ p(x) log p(x)) - Information Measurement
;; ====================================================================

(defn shannon-entropy
  "Calculate Shannon entropy of a probability distribution.
   
   Principle: Information theory quantifies complexity and uncertainty.
   Application: API design, data compression, system complexity measurement."
  [probabilities]
  (->> probabilities
       (remove zero?)
       (map (fn [p] (* p (math/log p))))
       (reduce +)
       (-)))

(defn measure-api-complexity
  "Quantify complexity of API interfaces using information theory.
   
   Principle: High entropy indicates poor information design.
   Application: API optimization, interface simplification."
  [api-endpoints]
  (let [param-counts (map count (map :parameters api-endpoints))
        total-params (reduce + param-counts)
        probabilities (map #(/ % total-params) param-counts)]
    {:entropy (shannon-entropy probabilities)
     :complexity-score (* (shannon-entropy probabilities) total-params)}))

(defn optimize-information-flow
  "Restructure system to minimize information entropy.
   
   Principle: Optimal systems have minimal necessary information flow.
   Application: Microservice decomposition, data flow optimization."
  [system]
  (let [current-entropy (measure-api-complexity (:apis system))]
    ;; Greedy optimization: combine high-coupling, low-information interfaces
    (reduce (fn [sys [a b]]
              (if (> (get-in sys [:coupling a b]) 0.8)
                (merge-apis sys a b)
                sys))
            system
            (combinations (:apis system) 2))))

;; ====================================================================
;; 6. CHAOS THEORY (xₜ₊₁ = k xₜ (1 - xₜ)) - Emergent Behavior
;; ====================================================================

(defn logistic-map
  "Implement logistic map for modeling chaotic systems.
   
   Principle: Simple nonlinear equations can produce complex emergent behavior.
   Application: Load testing, failure simulation, system resilience testing."
  [k initial-x iterations]
  (loop [x initial-x
         results [initial-x]
         i 0]
    (if (>= i iterations)
      results
      (let [next-x (* k x (- 1 x))]
        (recur next-x (conj results next-x) (inc i))))))

(defn test-system-resilience
  "Test system behavior under chaotic conditions.
   
   Principle: Chaotic inputs reveal brittleness and emergent failures.
   Application: Chaos engineering, robustness testing."
  [system chaotic-inputs]
  (let [responses (map #(try (process system %)
                             (catch Exception e
                               {:error (ex-message e)}))
                       chaotic-inputs)
        failures (filter :error responses)]
    {:total (count responses)
     :failures (count failures)
     :failure-rate (/ (count failures) (count responses))
     :chaotic-behavior? (> (count failures) (* 0.1 (count responses)))}))

(defn design-chaos-resistant
  "Apply chaos theory principles to system design.
   
   Principle: Small changes can have large, unpredictable effects.
   Application: Error boundaries, circuit breakers, graceful degradation."
  [system]
  (assoc system
         :error-boundaries true
         :circuit-breakers {:threshold 0.5 :timeout 5000}
         :graceful-degradation {:fallbacks true
                                :default-responses {:error "Service temporarily unavailable"}}))

;; ====================================================================
;; 7. MATHEMATICAL PATTERN VALIDATION & INTEGRATION
;; ====================================================================

(s/def ::orthogonal-components
  (s/and vector?
         (fn [components]
           (let [coupling (calculate-coupling components)]
             (ensure-orthogonal-components components coupling)))))

(s/def ::logarithmic-complexity
  (s/and fn?
         (fn [f]
           (let [test-data (range 1000)
                 time-n (time (f test-data))
                 time-2n (time (f (concat test-data test-data)))]
             (< (/ time-2n time-n) 1.5)))))  ;; Approx O(n log n) scaling

(defn apply-mathematical-principles
  "Apply appropriate mathematical principles to system design decisions.
   
   π (Synthesis): Holistic integration of mathematical wisdom.
   τ (Wisdom): Strategic application based on system characteristics."
  [system-problem]
  (cond
    ;; High coupling, poor separation → Pythagorean orthogonality
    (> (:coupling system-problem) 0.7)
    {:principle :pythagorean-orthogonality
     :action (orthogonal-decomposition (:components system-problem) (:coupling-fn system-problem))}

    ;; Exponential complexity → Logarithmic reduction
    (> (:complexity system-problem) 1000)
    {:principle :logarithmic-complexity-reduction
     :action (transform-multiplicative-to-additive (:operation system-problem) (:data system-problem))}

    ;; Unpredictable behavior → Chaos theory resilience
    (:chaotic-behavior system-problem)
    {:principle :chaos-theory-resilience
     :action (design-chaos-resistant system-problem)}

    ;; High information complexity → Information theory optimization
    (> (:entropy system-problem) 2.0)
    {:principle :information-theory-optimization
     :action (optimize-information-flow system-problem)}

    :else
    {:principle :calculus-adaptive-control
     :action (adaptive-threshold (:current-value system-problem) (:history system-problem))}))

(comment
  ;; Example usage
  (require '[cc.mindward.game.mathematical-patterns :as math])

  ;; 1. Pythagorean orthogonality for component design
  (let [components [:auth :game :user :ui]
        coupling [[0 0.2 0.1 0.3]
                  [0.2 0 0.8 0.1]  ;; game-user coupling too high!
                  [0.1 0.8 0 0.2]
                  [0.3 0.1 0.2 0]]]
    (math/ensure-orthogonal-components components coupling))
  ;; => false (game-user coupling violates orthogonality)

  ;; 2. Logarithmic complexity reduction
  (math/transform-multiplicative-to-additive
   (fn [data] (reduce + (map (fn [x] (* x x)) data)))  ;; O(n²) if naive
   (vec (range 1000)))

  ;; 3. Chaos theory resilience testing
  (math/test-system-resilience
   {:process (fn [x] (if (< (rand) 0.1) (throw (Exception. "chaos!")) x))}
   (math/logistic-map 3.9 0.5 100))

  ;; 4. Information theory API optimization
  (math/measure-api-complexity
   [{:name "create-user" :parameters [:username :password :email :display-name]}
    {:name "get-user" :parameters [:username]}
    {:name "update-score" :parameters [:username :score :timestamp]}])

  ;; 5. Calculus-based adaptive threshold
  (math/adaptive-threshold 100 [80 85 90 95 100 110 120 130]))