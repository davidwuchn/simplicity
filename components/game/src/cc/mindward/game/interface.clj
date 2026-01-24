(ns cc.mindward.game.interface
  "Game engine interface - Conway's Game of Life with musical integration.

   φ (Vitality): Game state evolves organically following cellular automata rules.
   ∃ (Truth): Musical triggers are deterministic based on pattern emergence."
  (:require [cc.mindward.game.impl :as impl]
            [cc.mindward.game.config :as config]
            [clojure.spec.alpha :as s]))

;; ------------------------------------------------------------
;; Domain Specs (π Synthesis)
;; ------------------------------------------------------------

(s/def :game/id keyword?)
(s/def :game/coordinate (s/tuple int? int?))
(s/def :game/board (s/coll-of :game/coordinate :kind set?))
(s/def :game/generation (s/and int? #(>= % 0)))
(s/def :game/score (s/and int? #(>= % 0)))
(s/def :game/trigger keyword?)
(s/def :game/params map?)

(s/def :game/musical-trigger
  (s/keys :req-un [:game/trigger :game/params]))

(s/def :game/healthy? boolean?)
(s/def :game/details map?)

(s/def :game/health-status
  (s/keys :req [:game/healthy?] :opt [:game/details]))

;; ------------------------------------------------------------
;; Domain Model (π Synthesis)
;; ------------------------------------------------------------

;; Board represented as set of living cell coordinates [x y]
;; Musical triggers use pattern classification thresholds

;; ------------------------------------------------------------
;; Config Accessors (μ Directness - single point of config access)
;; ------------------------------------------------------------

(defn board-max-x [] config/board-max-x)
(defn board-min-x [] config/board-min-x)
(defn board-max-y [] config/board-max-y)
(defn board-min-y [] config/board-min-y)

;; ------------------------------------------------------------
;; Query Operations (λ - pure lookups)
;; ------------------------------------------------------------

(s/fdef get-board
  :args (s/cat :game-id :game/id)
  :ret (s/nilable :game/board))

(defn get-board
  "Get the current game board state. Returns set of [x y] coordinates."
  [game-id]
  (impl/get-board game-id))

(s/fdef get-generation
  :args (s/cat :game-id :game/id)
  :ret :game/generation)

(defn get-generation
  "Get the current generation number for a game."
  [game-id]
  (impl/get-generation game-id))

(s/fdef get-score
  :args (s/cat :game-id :game/id)
  :ret (s/nilable :game/score))

(defn get-score
  "Calculate score based on board complexity and generation.
   Higher complexity + sustained life = higher score."
  [game-id]
  (impl/calculate-score game-id))

;; ------------------------------------------------------------
;; Command Operations (! suffix - side effects)
;; ------------------------------------------------------------

(s/fdef create-game!
  :args (s/cat :game-id :game/id :initial-board (s/nilable :game/board))
  :ret :game/board)

(defn create-game!
  "Create a new game with initial board state.
   initial-board: set of [x y] coordinates or nil for random generation."
  ([game-id] (create-game! game-id nil))
  ([game-id initial-board]
   (impl/create-game! game-id initial-board)))

(s/fdef evolve!
  :args (s/cat :game-id :game/id)
  :ret :game/board)

(defn evolve!
  "Evolve the game board one generation using Conway's rules.
   Returns the new board state."
  [game-id]
  (impl/evolve! game-id))

(s/fdef clear-cells!
  :args (s/cat :game-id :game/id :cells :game/board)
  :ret :game/board)

(defn clear-cells!
  "Clear specific cells from the board.
   cells: set of [x y] coordinates to remove."
  [game-id cells]
  (impl/clear-cells! game-id cells))

(s/fdef add-cells!
  :args (s/cat :game-id :game/id :cells :game/board)
  :ret :game/board)

(defn add-cells!
  "Add living cells to the board.
   cells: set of [x y] coordinates to add."
  [game-id cells]
  (impl/add-cells! game-id cells))

;; ------------------------------------------------------------
;; Musical Integration (∃ Truth - deterministic pattern mapping)
;; ------------------------------------------------------------

(s/fdef get-musical-triggers
  :args (s/cat :game-id :game/id)
  :ret (s/coll-of :game/musical-trigger))

(defn get-musical-triggers
  "Analyze board and return musical trigger events.
   Maps emergent patterns to audio parameters:
   - Oscillator density → harmonic complexity
   - Cell births/deaths → envelope triggers
   - Stable patterns → sustained drones
   - Chaos → noise bursts

   Returns vector of {:trigger :params} maps."
  [game-id]
  (impl/generate-musical-triggers game-id))

;; ------------------------------------------------------------
;; Lifecycle Management (τ Wisdom)
;; ------------------------------------------------------------

(s/fdef initialize!
  :args (s/cat)
  :ret nil?)

(defn initialize!
  "Initialize the game engine. Must be called once at startup.
   Starts background cleanup of stale game sessions."
  []
  (impl/initialize!))

(s/fdef cleanup-stale-games!
  :args (s/cat)
  :ret int?)

(defn cleanup-stale-games!
  "Manually trigger cleanup of stale games.
   Returns the number of games removed."
  []
  (impl/cleanup-stale-games!))

(s/fdef stop-cleanup-scheduler!
  :args (s/cat)
  :ret nil?)

(defn stop-cleanup-scheduler!
  "Stop the background cleanup scheduler. Call on shutdown."
  []
  (impl/stop-cleanup-scheduler!))

(s/fdef health-check
  :args (s/cat)
  :ret :game/health-status)

(defn health-check
  "Check game engine health status.
   Returns {:healthy? true/false :details {...}}"
  []
  (impl/health-check))
