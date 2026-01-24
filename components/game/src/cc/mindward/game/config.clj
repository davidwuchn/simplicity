(ns cc.mindward.game.config
  "Game configuration and domain constants.
   
   ∃ (Truth): All magic numbers extracted to named constants.
   e (Purpose): Single source of truth for game mechanics.")

;; ------------------------------------------------------------
;; Conway's Game of Life Rules
;; ------------------------------------------------------------

(def conway-birth-neighbors
  "Number of neighbors required for a dead cell to become alive."
  3)

(def conway-survival-min
  "Minimum neighbors for a living cell to survive."
  2)

(def conway-survival-max
  "Maximum neighbors for a living cell to survive."
  3)

;; ------------------------------------------------------------
;; Board Boundaries
;; ------------------------------------------------------------

(def board-max-x
  "Maximum x-coordinate for board cells."
  100)

(def board-max-y
  "Maximum y-coordinate for board cells."
  100)

(def board-min-x
  "Minimum x-coordinate for board cells."
  -100)

(def board-min-y
  "Minimum y-coordinate for board cells."
  -100)

;; ------------------------------------------------------------
;; Scoring System
;; ------------------------------------------------------------

(def score-generation-cap
  "Maximum generation count used for complexity scoring."
  100)

(def score-stability-threshold
  "Generation count required to earn stability bonus."
  10)

(def score-stability-bonus
  "Points awarded when stability threshold is reached."
  100)

(def score-minimum
  "Minimum possible score (prevents zero/negative scores)."
  1)

;; ------------------------------------------------------------
;; Musical Triggers (Density Thresholds)
;; ------------------------------------------------------------

(def music-density-high-threshold
  "Cell count threshold for high-density musical trigger."
  50)

(def music-density-mid-threshold
  "Cell count threshold for mid-density musical trigger."
  20)

(def music-max-cells-normalization
  "Divisor for normalizing cell count to 0.0-1.0 density."
  100.0)

;; Musical frequencies (Hz)
(def music-freq-high-density 440)
(def music-freq-mid-density 220)
(def music-freq-drone 65)

;; Musical amplitudes (0.0-1.0)
(def music-amp-high-density 0.8)
(def music-amp-mid-density 0.6)
(def music-amp-drone 0.3)

;; ------------------------------------------------------------
;; Lifecycle & Cleanup
;; ------------------------------------------------------------

(def game-ttl-ms
  "Time-to-live for inactive games (milliseconds)."
  (* 60 60 1000)) ; 1 hour

(def cleanup-interval-minutes
  "Interval between cleanup scheduler runs (minutes)."
  10)

(def cleanup-initial-delay-minutes
  "Initial delay before first cleanup run (minutes)."
  10)

;; ------------------------------------------------------------
;; Resource Limits (∀ Vigilance)
;; ------------------------------------------------------------

(def max-games
  "Maximum number of concurrent game sessions to prevent unbounded memory growth."
  1000)
